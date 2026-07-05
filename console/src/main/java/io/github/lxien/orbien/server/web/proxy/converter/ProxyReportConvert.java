/*
 *    Copyright 2026 lxien
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package io.github.lxien.orbien.server.web.proxy.converter;

import io.github.lxien.orbien.common.utils.StringUtils;
import io.github.lxien.orbien.core.domain.HealthCheckConfig;
import io.github.lxien.orbien.core.enums.*;
import io.github.lxien.orbien.core.enums.*;
import io.github.lxien.orbien.core.message.Message;
import io.github.lxien.orbien.core.utils.BandwidthParser;
import io.github.lxien.orbien.server.event.ProxyAddEvent;
import io.github.lxien.orbien.server.web.entity.*;
import io.github.lxien.orbien.server.web.entity.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ProxyReportConvert {

    public ProxyDO toProxyDO(String agentId, String proxyId, Message.Proxy proxy, ProxyAddEvent event) {
        ProtocolType protocol = ProtocolType.fromName(proxy.getProtocol().name());
        if (protocol == null) {
            throw new IllegalArgumentException("未知协议类型: " + proxy.getProtocol());
        }

        ProxyDO proxyDO = new ProxyDO();
        proxyDO.setId(proxyId);
        proxyDO.setAgentId(agentId);
        proxyDO.setName(proxy.getName());
        proxyDO.setProtocol(protocol);
        proxyDO.setStatus(proxy.getEnabled() ? ProxyStatus.OPEN : ProxyStatus.CLOSED);
        proxyDO.setSourceType(ProxySourceType.AGENT);
        proxyDO.setLoadBalanceStrategy(toLoadBalanceType(proxy));

        if (protocol.isTcpOrUdp()) {
            applyPorts(proxyDO, proxy, event.getListenPort());
        }

        if (proxy.hasTransport()) {
            applyTransport(proxyDO, proxy.getTransport());
        }
        if (proxy.hasBandwidth()) {
            applyBandwidth(proxyDO, proxy.getBandwidth());
        }
        if (protocol.isHttps()) {
            proxyDO.setForceHttps(proxy.getForceHttps());
        }
        return proxyDO;
    }

    private void applyPorts(ProxyDO proxyDO, Message.Proxy proxy, Integer listenPort) {
        proxyDO.setListenPort(listenPort);
        if (proxy.hasRemotePort() && proxy.getRemotePort() > 0) {
            proxyDO.setRemotePort(proxy.getRemotePort());
        }
    }

    public void applyTransport(ProxyDO proxyDO, Message.Transport transport) {
        if (transport.hasMultiplex()) {
            proxyDO.setMultiplex(transport.getMultiplex());
        }
        if (transport.hasEncrypt()) {
            proxyDO.setEncrypt(transport.getEncrypt());
        }
        if (transport.hasCompress()) {
            proxyDO.setCompress(transport.getCompress());
        }
        if (transport.hasProtocol()) {
            TransportProtocol protocol = TransportProtocol.fromName(transport.getProtocol());
            if (protocol == null) {
                throw new IllegalArgumentException("不支持的传输协议: " + transport.getProtocol());
            }
            proxyDO.setTransportProtocol(protocol);
        }
    }

    public void applyBandwidth(ProxyDO proxyDO, Message.Bandwidth bandwidth) {
        if (bandwidth.hasLimit()) {
            proxyDO.setLimitTotal(BandwidthParser.parseToBps(bandwidth.getLimit()));
        }
        if (bandwidth.hasLimitIn()) {
            proxyDO.setLimitIn(BandwidthParser.parseToBps(bandwidth.getLimitIn()));
        }
        if (bandwidth.hasLimitOut()) {
            proxyDO.setLimitOut(BandwidthParser.parseToBps(bandwidth.getLimitOut()));
        }
    }

    public AccessControlDO toAccessControlDO(Message.AccessControl accessControl, String proxyId) {
        AccessControlDO accessControlDO = new AccessControlDO();
        accessControlDO.setProxyId(proxyId);
        accessControlDO.setEnabled(accessControl.getEnabled());
        accessControlDO.setMode(toAccessControlMode(accessControl.getMode()));
        return accessControlDO;
    }

    public List<AccessControlRuleDO> toAccessControlRuleDOList(Message.AccessControl accessControl, String proxyId) {
        List<AccessControlRuleDO> rules = new ArrayList<>();
        for (String cidr : accessControl.getAllowList()) {
            rules.add(buildAccessControlRule(proxyId, cidr, AccessControl.ALLOW));
        }
        for (String cidr : accessControl.getDenyList()) {
            rules.add(buildAccessControlRule(proxyId, cidr, AccessControl.DENY));
        }
        return rules;
    }

    public List<ProxyTargetDO> toProxyTargetDOList(List<Message.Target> targets, String proxyId) {
        if (targets == null || targets.isEmpty()) {
            return List.of();
        }
        List<ProxyTargetDO> result = new ArrayList<>(targets.size());
        for (Message.Target target : targets) {
            ProxyTargetDO targetDO = new ProxyTargetDO();
            targetDO.setProxyId(proxyId);
            targetDO.setHost(target.getHost());
            targetDO.setPort(target.getPort());
            if (target.hasName()) {
                targetDO.setName(target.getName());
            }
            if (target.hasWeight()) {
                targetDO.setWeight(target.getWeight());
            }
            result.add(targetDO);
        }
        return result;
    }

    public HealthCheckDO toHealthCheckDO(Message.HealthCheck healthCheck, String proxyId) {
        HealthCheckDO healthCheckDO = new HealthCheckDO();
        healthCheckDO.setProxyId(proxyId);
        healthCheckDO.setEnabled(healthCheck.getEnabled());
        healthCheckDO.setType(toHealthCheckType(healthCheck.getType()));
        healthCheckDO.setInterval(resolvePositive(healthCheck.getInterval(), HealthCheckConfig.DEFAULT_INTERVAL));
        healthCheckDO.setTimeout(resolvePositive(healthCheck.getTimeout(), HealthCheckConfig.DEFAULT_TIMEOUT));
        healthCheckDO.setMaxFailed(resolvePositive(healthCheck.getMaxFailed(), HealthCheckConfig.DEFAULT_MAX_FAILED));
        healthCheckDO.setPath(resolveHealthCheckPath(healthCheck));
        return healthCheckDO;
    }

    public List<BasicUserDO> toBasicUserDOList(Message.BasicAuth basicAuth, String proxyId) {
        List<BasicUserDO> users = new ArrayList<>();
        for (Message.HttpUser httpUser : basicAuth.getHttpUsersList()) {
            users.add(new BasicUserDO(proxyId, httpUser.getUsername(), httpUser.getPassword()));
        }
        return users;
    }

    private AccessControlRuleDO buildAccessControlRule(String proxyId, String cidr, AccessControl mode) {
        AccessControlRuleDO ruleDO = new AccessControlRuleDO();
        ruleDO.setProxyId(proxyId);
        ruleDO.setCidr(cidr);
        ruleDO.setMode(mode);
        return ruleDO;
    }

    private LoadBalanceType toLoadBalanceType(Message.Proxy proxy) {
        if (!proxy.hasLoadBalanceStrategy()) {
            return LoadBalanceType.ROUND_ROBIN;
        }
        return switch (proxy.getLoadBalanceStrategy()) {
            case WEIGHT -> LoadBalanceType.WEIGHT;
            case RANDOM -> LoadBalanceType.RANDOM;
            case LEAST_CONN -> LoadBalanceType.LEAST_CONN;
            default -> LoadBalanceType.ROUND_ROBIN;
        };
    }

    private AccessControl toAccessControlMode(Message.AccessMode mode) {
        return switch (mode) {
            case ALLOW -> AccessControl.ALLOW;
            default -> AccessControl.DENY;
        };
    }

    private HealthCheckType toHealthCheckType(Message.HealthCheckType type) {
        if (type == Message.HealthCheckType.HEALTH_CHECK_TYPE_HTTP) {
            return HealthCheckType.HTTP;
        }
        return HealthCheckType.TCP;
    }

    private int resolvePositive(int value, int defaultValue) {
        return value > 0 ? value : defaultValue;
    }

    private String resolveHealthCheckPath(Message.HealthCheck healthCheck) {
        if (StringUtils.hasText(healthCheck.getPath())) {
            return healthCheck.getPath();
        }
        return toHealthCheckType(healthCheck.getType()).isHttpCheck()
                ? HealthCheckConfig.DEFAULT_PATH
                : "/";
    }
}
