/*
 *
 *  *    Copyright 2026 xiaoniucode
 *  *
 *  *    Licensed under the Apache License, Version 2.0 (the "License");
 *  *    you may not use this file except in compliance with the License.
 *  *    You may obtain a copy of the License at
 *  *
 *  *        http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  *    Unless required by applicable law or agreed to in writing, software
 *  *    distributed under the License is distributed on an "AS IS" BASIS,
 *  *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  *    See the License for the specific language governing permissions and
 *  *    limitations under the License.
 *
 */

package com.xiaoniucode.etp.client.utils;

import com.xiaoniucode.etp.common.utils.StringUtils;
import com.xiaoniucode.etp.core.domain.*;
import com.xiaoniucode.etp.core.enums.LoadBalanceType;
import com.xiaoniucode.etp.core.enums.ProtocolType;
import com.xiaoniucode.etp.core.message.Message;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ProxyConfigAssembler {
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(ProxyConfigAssembler.class);

    private ProxyConfigAssembler() {

    }

    public static Message.Proxy toProto(ProxyConfig config) {
        ProtocolType protocol = config.getProtocol();
        Message.Proxy.Builder proxyBuilder = Message.Proxy.newBuilder();
        List<Message.Target> targets = config.getTargets().stream().map(t -> {
                    Message.Target.Builder target = Message.Target.newBuilder()
                            .setHost(t.getHost())
                            .setPort(t.getPort())
                            .setWeight(t.getWeight());
                    if (StringUtils.hasText(t.getName())) {
                        target.setName(t.getName());
                    }
                    if (t.getWeight() != null) {
                        target.setWeight(t.getWeight());
                    }
                    return target.build();
                }
        ).collect(Collectors.toList());
        proxyBuilder.setName(config.getName())
                .addAllTargets(targets)
                .setForceHttps(config.getForceHttps())
                .setProtocol(Message.ProtocolType.valueOf(config.getProtocol().name()));

        if (config.getStatus().isOpen()) {
            proxyBuilder.setEnabled(true);
        }

        switch (protocol) {
            case TCP:
                if (config.getRemotePort() != null) {
                    proxyBuilder.setRemotePort(config.getRemotePort());
                }
                break;
            case HTTP:
            case HTTPS:
                //域名配置
                RouteConfig domainInfo = config.getRouteConfig();
                if (domainInfo != null) {
                    Set<String> customDomains = domainInfo.getCustomDomains();
                    Boolean autoDomain = domainInfo.getAutoDomain();
                    Set<String> subDomains = domainInfo.getSubDomains();
                    Message.DomainConfig domainReq = Message.DomainConfig.newBuilder()
                            .setAutoDomain(autoDomain)
                            .addAllCustomDomains(customDomains)
                            .addAllSubDomains(subDomains).build();
                    proxyBuilder.setDomain(domainReq);
                }

                //Basic Auth 认证
                if (config.hasBasicAuth()) {
                    BasicAuthConfig basicAuth = config.getBasicAuth();
                    Message.BasicAuth.Builder basicAuthBuilder = Message.BasicAuth.newBuilder()
                            .setEnabled(basicAuth.isEnabled());
                    Set<HttpUser> users = basicAuth.getUsers();
                    if (users != null && !users.isEmpty()) {
                        for (HttpUser user : users) {
                            Message.HttpUser httpUser = Message.HttpUser.newBuilder()
                                    .setUsername(user.getUsername())
                                    .setPassword(user.getPassword())
                                    .build();
                            basicAuthBuilder.addHttpUsers(httpUser);
                        }
                    }
                    proxyBuilder.setBasicAuth(basicAuthBuilder);
                }
                //HTTPS SSL证书
                if (config.isHttps()) {
                    SslConfig sslConfig = config.getSslConfig();
                    if (sslConfig != null) {
                        try {
                            String keyPem = Files.readString(new File(sslConfig.getKeyFile()).toPath(), StandardCharsets.UTF_8);
                            String certChainPem = Files.readString(new File(sslConfig.getCertFile()).toPath(), StandardCharsets.UTF_8);
                            Message.SslCert.Builder sslInfoBuilder = Message.SslCert.newBuilder()
                                    .setPrivateKeyPem(keyPem)
                                    .setCertChainPem(certChainPem);
                            proxyBuilder.setSslCert(sslInfoBuilder);
                        } catch (Exception e) {
                            logger.error(e);
                        }
                    }
                }
                break;
        }
        //传输
        if (config.hasTransport()) {
            Message.Transport.Builder builder = Message.Transport.newBuilder();
            TransportCustomConfig transport = config.getTransport();
            Boolean encrypt = transport.getEncrypt();
            Boolean compress = transport.getCompress();
            Boolean mux = transport.getMultiplex();
            if (encrypt != null) {
                builder.setEncrypt(encrypt);
            }
            if (compress != null) {
                builder.setCompress(compress);
            }
            if (mux != null) {
                builder.setMultiplex(mux);
            }
            proxyBuilder.setTransport(builder.build());
        }

        //访问控制
        if (config.hasAccessControl()) {
            AccessControlConfig access = config.getAccessControl();
            Message.AccessControl.Builder accessControlbuilder = Message.AccessControl
                    .newBuilder()
                    .setEnabled(access.isEnabled())
                    .setMode(Message.AccessMode.valueOf(access.getMode().name()));
            if (access.hasAllow()) {
                Set<String> allow = access.getAllow();
                accessControlbuilder.addAllAllow(allow);
            }
            if (access.hasDeny()) {
                Set<String> deny = access.getDeny();
                accessControlbuilder.addAllDeny(deny);
            }
            proxyBuilder.setAccessControl(accessControlbuilder.build());
        }
        //带宽限制
        if (config.hasBandwidthLimit()) {
            BandwidthConfig bandwidth = config.getBandwidth();
            Message.Bandwidth.Builder bw = Message.Bandwidth.newBuilder();
            if (bandwidth.hasLimitConfigured()) {
                bw.setLimit(bandwidth.getLimitTotal());
            }
            if (bandwidth.hasLimitInConfigured()) {
                bw.setLimitIn(bandwidth.getLimitIn());
            }
            if (bandwidth.hasLimitOutConfigured()) {
                bw.setLimitOut(bandwidth.getLimitOut());
            }
            proxyBuilder.setBandwidth(bw.build());
        }
        //负载均衡
        if (config.hasLoadBalance()) {
            Message.LoadBalance.Builder loadBalanceBuilder = Message.LoadBalance.newBuilder();
            if (config.getLoadBalance().hasStrategy()) {
                Message.LoadBalanceStrategy strategy = toProtoType(config.getLoadBalance().getStrategy());
                loadBalanceBuilder.setStrategy(strategy);
            }
            proxyBuilder.setLoadBalance(loadBalanceBuilder.build());
        }

        return proxyBuilder.build();
    }

    private static Message.LoadBalanceStrategy toProtoType(LoadBalanceType strategy) {
        switch (strategy) {
            case ROUND_ROBIN:
                return Message.LoadBalanceStrategy.ROUND_ROBIN;
            case WEIGHT:
                return Message.LoadBalanceStrategy.WEIGHT;
            case RANDOM:
                return Message.LoadBalanceStrategy.RANDOM;
            case LEAST_CONN:
                return Message.LoadBalanceStrategy.LEAST_CONN;
            default:
                throw new IllegalArgumentException("未知负载均衡策略: " + strategy);
        }
    }

    public static ProxyConfigExt toDomain(Message.ProxyRuntime p) {
        return null;
    }
}
