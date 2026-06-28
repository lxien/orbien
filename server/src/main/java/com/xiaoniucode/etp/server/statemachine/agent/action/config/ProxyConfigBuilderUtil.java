///*
// *    Copyright 2026 xiaoniucode
// *
// *    Licensed under the Apache License, Version 2.0 (the "License");
// *    you may not use this file except in compliance with the License.
// *    You may obtain a copy of the License at
// *
// *        http://www.apache.org/licenses/LICENSE-2.0
// *
// *    Unless required by applicable law or agreed to in writing, software
// *    distributed under the License is distributed on an "AS IS" BASIS,
// *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// *    See the License for the specific language governing permissions and
// *    limitations under the License.
// */
//
//package com.xiaoniucode.etp.server.statemachine.agent.action.config;
//
//import com.google.protobuf.ProtocolStringList;
//import com.xiaoniucode.etp.core.domain.*;
//import com.xiaoniucode.etp.core.enums.*;
//import com.xiaoniucode.etp.core.message.Message;
//import org.springframework.security.crypto.password.PasswordEncoder;
//
//import java.util.HashSet;
//import java.util.List;
//import java.util.Set;
//import java.util.stream.Collectors;
//
//public class ProxyConfigBuilderUtil {
//    private ProxyConfigBuilderUtil() {
//
//    }
//    public static ProxyConfig buildProxyConfig(Message.NewProxy proxy, PasswordEncoder passwordEncoder) {
//        ProxyConfig proxyConfig = new ProxyConfig();
//        proxyConfig.setSourceType(ProxySourceType.AGENT);
//        proxyConfig.setName(proxy.getName());
//        proxyConfig.setForceHttps(proxy.getForceHttps());
//        List<Target> targets = proxy.getTargetsList().stream().map(p -> {
//            Target target = new Target();
//            target.setHost(p.getHost());
//            target.setPort(p.getPort());
//            if (p.hasName()) {
//                target.setName(p.getName());
//            }
//            if (p.hasWeight()) {
//                target.setWeight(p.getWeight());
//            }
//            return target;
//        }).toList();
//        proxyConfig.addTargets(targets);
//        if (proxy.hasRemotePort()) {
//            proxyConfig.setRemotePort(proxy.getRemotePort());
//        }
//        proxyConfig.setProtocol(ProtocolType.getByName(proxy.getProtocol().name()));
//        if (proxy.hasEnable() && proxy.getEnable()) {
//            proxyConfig.setStatus(ProxyStatus.OPEN);
//        }
//        if (proxy.hasDomain()) {
//            Message.DomainInfo domainInfo = proxy.getDomain();
//            RouteConfig routeConfig = new RouteConfig();
//            if (domainInfo.hasAutoDomain()) {
//                routeConfig.setAutoDomain(domainInfo.getAutoDomain());
//            }
//            ProtocolStringList customDomainsList = domainInfo.getCustomDomainsList();
//            if (!customDomainsList.isEmpty()) {
//                routeConfig.getCustomDomains().addAll(customDomainsList);
//            }
//            ProtocolStringList subDomainsList = domainInfo.getSubDomainsList();
//            if (!subDomainsList.isEmpty()) {
//                routeConfig.getSubDomains().addAll(subDomainsList);
//            }
//            proxyConfig.setRouteConfig(routeConfig);
//        }
//        if (proxy.hasTransport()) {
//            TransportCustomConfig transportCustomConfig = new TransportCustomConfig();
//            Message.Transport transport = proxy.getTransport();
//            if (transport.hasMux()) {
//                transportCustomConfig.setMultiplex(transport.getMux());
//            }
//            if (transport.hasCompress()) {
//                transportCustomConfig.setCompress(transport.getCompress());
//            }
//            if (transport.hasEncrypt()) {
//                transportCustomConfig.setEncrypt(transport.getEncrypt());
//            }
//            proxyConfig.setTransport(transportCustomConfig);
//        }
//
//        if (proxy.hasAccessControl()) {
//            Message.AccessControl accessControl = proxy.getAccessControl();
//            boolean enable = accessControl.getEnable();
//            AccessControl accessControlMode = AccessControl.fromValue(accessControl.getMode().name());
//            Set<String> allow = new HashSet<>(accessControl.getAllowList());
//            Set<String> deny = new HashSet<>(accessControl.getDenyList());
//            proxyConfig.setAccessControl(new AccessControlConfig(enable, accessControlMode, allow, deny));
//        }
//        if (proxyConfig.getProtocol().isHttp() && proxy.hasBasicAuth()) {
//            Message.BasicAuth basicAuth = proxy.getBasicAuth();
//            Set<HttpUser> users = basicAuth.getHttpUsersList().stream()
//                    .map(httpUser -> new HttpUser(httpUser.getUser(), passwordEncoder.encode(httpUser.getPass())))
//                    .collect(Collectors.toSet());
//
//            BasicAuthConfig basicAuthConfig = proxyConfig.getOrCreateBasicAuthConfig();
//            basicAuthConfig.setEnabled(basicAuth.getEnable());
//            basicAuthConfig.addUsers(users);
//            proxyConfig.setBasicAuth(basicAuthConfig);
//        }
//        if (proxy.hasBandwidth()) {
//            Message.Bandwidth bandwidth = proxy.getBandwidth();
//            BandwidthConfig bandwidthConfig = new BandwidthConfig(bandwidth.getLimit(),
//                    bandwidth.getLimitIn(),
//                    bandwidth.getLimitOut());
//            proxyConfig.setBandwidth(bandwidthConfig);
//        }
//        if (proxy.hasLoadBalance()) {
//            LoadBalanceConfig loadBalanceConfig = new LoadBalanceConfig();
//            Message.LoadBalance loadBalance = proxy.getLoadBalance();
//            if (loadBalance.hasStrategy()) {
//                loadBalanceConfig.setStrategy(toJavaType(loadBalance.getStrategy()));
//            }
//            proxyConfig.setLoadBalance(loadBalanceConfig);
//        }
//        return proxyConfig;
//    }
//    private static LoadBalanceType toJavaType(Message.LoadBalanceStrategy strategy) {
//        if (strategy == null) {
//            return LoadBalanceConfig.DEFAULT_STRATEGY;
//        }
//        return switch (strategy) {
//            case WEIGHT -> LoadBalanceType.WEIGHT;
//            case RANDOM -> LoadBalanceType.RANDOM;
//            case LEAST_CONN -> LoadBalanceType.LEAST_CONN;
//            default -> LoadBalanceConfig.DEFAULT_STRATEGY;
//        };
//    }
//}
