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

package io.github.lxien.orbien.server.web.proxy.repository.assembler;

import io.github.lxien.orbien.core.domain.*;
import io.github.lxien.orbien.core.domain.*;
import io.github.lxien.orbien.core.enums.AccessControl;
import io.github.lxien.orbien.core.enums.DomainType;
import io.github.lxien.orbien.server.web.entity.*;
import io.github.lxien.orbien.server.web.proxy.converter.ProxyModelConvert;
import io.github.lxien.orbien.server.web.dto.proxy.ProxyDetailQueryResult;
import io.github.lxien.orbien.server.web.entity.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class ProxyConfigAssembler {
    @Autowired
    private ProxyModelConvert proxyModelConvert;

    /**
     * 组装完整的代理扩展配置。
     */
    public ProxyConfigExt assembleExt(ProxyDetailQueryResult detail, ProxyRelations relations) {
        if (detail == null || detail.getProxyDO() == null) {
            return null;
        }
        ProxyConfig config = assembleBase(detail);
        if (config == null) {
            return null;
        }

        assembleTransport(config, detail.getProxyDO());
        assembleTargets(config, relations.targets());
        assembleAccessControlRules(config, relations.accessControlRules());

        if (config.isHttpOrHttps()) {
            assembleDomains(config, relations.domains());
            if (config.hasBasicAuth()) {
                assembleBasicAuthUsers(config, relations.basicUsers());
            }
        }
        if (config.isFile()) {
            assembleDomains(config, relations.domains());
            assembleFileShareAuth(config, relations.fileShareAuth(), relations.fileShareUsers());
            assembleFileShareLimits(config, relations.fileShareLimits());
        }
        if (config.isSocks5()) {
            assembleSocks5Auth(config, relations.socks5Auth(), relations.socks5Users());
        }

        if (!config.isSocks5() && !config.isFile()) {
            assembleHealthCheck(config, relations.healthCheck());
        }
        return ProxyConfigExt.of(config, toDomainInfos(relations.domains()));
    }

    public ProxyConfig assembleBase(ProxyDetailQueryResult result) {
        if (result == null || result.getProxyDO() == null) {
            return null;
        }
        AgentDO agentDO = result.getAgentDO();
        ProxyDO proxyDO = result.getProxyDO();

        ProxyConfig config = proxyModelConvert.toProxyConfig(proxyDO);
        if (agentDO != null) {
            config.setAgentId(agentDO.getId());
            config.setAgentType(agentDO.getAgentType());
        } else {
            config.setAgentId(proxyDO.getAgentId());
        }
        config.setProxyId(proxyDO.getId());
        config.setListenPort(config.getRemotePort());
        config.setInspectorEnabled(Boolean.TRUE.equals(proxyDO.getInspectorEnabled()));

        if (proxyDO.getLimitTotal() != null || proxyDO.getLimitIn() != null || proxyDO.getLimitOut() != null) {
            config.setBandwidth(new BandwidthConfig(proxyDO.getLimitTotal(), proxyDO.getLimitIn(), proxyDO.getLimitOut()));
        }

        AccessControlDO accessControlDO = result.getAccessControlDO();
        if (accessControlDO != null) {
            config.setAccessControl(proxyModelConvert.toAccessControlConfig(accessControlDO));
        }

        BasicAuthDO basicAuthDO = result.getBasicAuthDO();
        if (config.isHttpOrHttps() && basicAuthDO != null) {
            config.setBasicAuth(proxyModelConvert.toBasicAuthConfig(basicAuthDO));
        }
        return config;
    }

    public void assembleSocks5Auth(ProxyConfig config, Socks5AuthDO socks5AuthDO, List<Socks5UserDO> users) {
        if (socks5AuthDO == null) {
            return;
        }
        Socks5AuthConfig authConfig = proxyModelConvert.toSocks5AuthConfig(socks5AuthDO);
        if (!CollectionUtils.isEmpty(users)) {
            authConfig.addUsers(proxyModelConvert.toSocks5UserConfig(users));
        }
        config.setSocks5Auth(authConfig);
    }

    public void assembleFileShareAuth(ProxyConfig config, FileShareAuthDO fileShareAuthDO, List<FileShareUserDO> users) {
        if (fileShareAuthDO == null) {
            return;
        }
        FileShareAuthConfig authConfig = proxyModelConvert.toFileShareAuthConfig(fileShareAuthDO);
        if (!CollectionUtils.isEmpty(users)) {
            for (FileShareAuthConfig.FileShareUser user : proxyModelConvert.toFileShareUserConfig(users)) {
                authConfig.addUser(user);
            }
        }
        config.setFileShareAuth(authConfig);
    }

    public void assembleFileShareLimits(ProxyConfig config, FileShareLimitsDO limitsDO) {
        if (limitsDO == null) {
            return;
        }
        config.setFileShareLimits(proxyModelConvert.toFileShareLimitsConfig(limitsDO));
    }

    public void assembleTransport(ProxyConfig config, ProxyDO proxyDO) {
        if (proxyDO == null) {
            return;
        }
        if (proxyDO.getMultiplex() == null && proxyDO.getEncrypt() == null
                && proxyDO.getCompress() == null && proxyDO.getTransportProtocol() == null) {
            return;
        }
        config.setTransport(TransportCustomConfig.builder()
                .protocol(proxyDO.getTransportProtocol())
                .multiplex(proxyDO.getMultiplex())
                .encrypt(proxyDO.getEncrypt())
                .compress(proxyDO.getCompress())
                .build());
    }

    public void assembleTargets(ProxyConfig config, List<ProxyTargetDO> targets) {
        if (CollectionUtils.isEmpty(targets)) {
            return;
        }
        config.addTargets(proxyModelConvert.toTargetModel(targets));
    }

    public void assembleDomains(ProxyConfig config, List<ProxyDomainDO> domainDOs) {
        if (CollectionUtils.isEmpty(domainDOs)) {
            return;
        }
        RouteConfig routeConfig = new RouteConfig();
        for (ProxyDomainDO domainDO : domainDOs) {
            DomainType domainType = domainDO.getDomainType();
            if (domainType == null) {
                continue;
            }
            if (domainType.isAuto()) {
                routeConfig.setAutoDomain(true);
            } else if (domainType.isCustomDomain()) {
                routeConfig.addCustomDomain(domainDO.getDomain());
            } else if (domainType.isSubdomain()) {
                routeConfig.addSubDomain(domainDO.getDomain());
            }
        }
        config.setRouteConfig(routeConfig);
    }

    public void assembleBasicAuthUsers(ProxyConfig config, List<BasicUserDO> basicUsers) {
        if (CollectionUtils.isEmpty(basicUsers)) {
            return;
        }
        BasicAuthConfig basicAuth = config.getBasicAuth();
        if (basicAuth != null) {
            basicAuth.addUsers(proxyModelConvert.toBasicAuthUserConfig(basicUsers));
        }
    }

    public void assembleAccessControlRules(ProxyConfig config, List<AccessControlRuleDO> accessControlRuleDOS) {
        if (CollectionUtils.isEmpty(accessControlRuleDOS) || !config.hasAccessControl()) {
            return;
        }
        AccessControlConfig accessControl = config.getAccessControl();
        accessControlRuleDOS.forEach(rule -> {
            AccessControl mode = rule.getMode();
            if (mode.isAllowMode()) {
                accessControl.addAllow(rule.getCidr());
            } else if (mode.isDenyMode()) {
                accessControl.addDeny(rule.getCidr());
            }
        });
    }

    public void assembleHealthCheck(ProxyConfig config, HealthCheckDO healthCheckDO) {
        if (healthCheckDO == null) {
            return;
        }
        HealthCheckConfig healthCheck = new HealthCheckConfig();
        boolean enabled = Boolean.TRUE.equals(healthCheckDO.getEnabled()) && !config.isUdp();
        healthCheck.setEnabled(enabled);
        healthCheck.setType(healthCheckDO.getType());
        healthCheck.setInterval(healthCheckDO.getInterval());
        healthCheck.setTimeout(healthCheckDO.getTimeout());
        healthCheck.setMaxFailed(healthCheckDO.getMaxFailed());
        healthCheck.setPath(healthCheckDO.getPath());
        config.setHealthCheck(healthCheck);
    }

    public Set<DomainInfo> toDomainInfos(List<ProxyDomainDO> domainDOs) {
        if (CollectionUtils.isEmpty(domainDOs)) {
            return Set.of();
        }
        return domainDOs.stream()
                .map(domainDO -> new DomainInfo(domainDO.getRootDomain(), domainDO.getDomain(), domainDO.getDomainType()))
                .collect(Collectors.toSet());
    }

    public List<ProxyConfig> assembleList(List<ProxyDO> list) {
        return proxyModelConvert.toProxyConfig(list);
    }
}
