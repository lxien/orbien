/*
 *    Copyright 2026 xiaoniucode
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

package com.xiaoniucode.etp.server.statemachine.agent.action.config;

import com.xiaoniucode.etp.server.uid.UidGenerator;
import com.xiaoniucode.etp.common.utils.StringUtils;
import com.xiaoniucode.etp.core.domain.ProxyConfig;
import com.xiaoniucode.etp.core.domain.RouteConfig;
import com.xiaoniucode.etp.core.enums.DomainType;
import com.xiaoniucode.etp.server.config.AppConfig;
import com.xiaoniucode.etp.server.exceptions.EtpException;
import com.xiaoniucode.etp.server.manager.ProxyManager;
import com.xiaoniucode.etp.server.service.ProxyConfigService;
import com.xiaoniucode.etp.server.statemachine.agent.AgentInfo;
import com.xiaoniucode.etp.server.vhost.DomainGenerator;
import com.xiaoniucode.etp.server.vhost.DomainInfo;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * HTTP代理操作策略实现
 */
@Component
public class HttpProxyOperationStrategy implements ProxyConfigOperationStrategy {
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(HttpProxyOperationStrategy.class);

    private final ProxyManager proxyManager;
    private final DomainGenerator domainGenerator;
    @Autowired
    private UidGenerator uidGenerator;
    @Resource
    private AppConfig appConfig;

    @Autowired
    private ProxyConfigService proxyConfigService;

    public HttpProxyOperationStrategy(ProxyManager proxyManager, DomainGenerator domainGenerator) {
        this.proxyManager = proxyManager;
        this.domainGenerator = domainGenerator;
    }

    @Override
    public ProxyOperationResult create(ProxyConfig config, AgentInfo agentInfo) {
        String baseDomain = appConfig.getBaseDomain();
        logger.debug("创建HTTP代理: {}", config.getName());
        Set<DomainInfo> domains;

        RouteConfig routeConfig = config.getRouteConfig();
        DomainType domainType = routeConfig.getDomainType();
        if (domainType.isCustomDomain()) {
            domains = domainGenerator.generateCustomDomains(routeConfig.getCustomDomains());
        } else if (domainType.isAuto()) {
            if (!StringUtils.hasText(baseDomain)) {
                throw new EtpException("服务不支持自动生成域名");
            }
            DomainInfo domainInfo = domainGenerator.generateRandomSubdomain(baseDomain);
            domains = new HashSet<>();
            domains.add(domainInfo);
        } else {
            if (!StringUtils.hasText(baseDomain)) {
                throw new EtpException("不支持子域名");
            }
            Set<String> subDomains = routeConfig.getSubDomains();
            if (CollectionUtils.isEmpty(subDomains)) {
                throw new EtpException("至少指定一个子域名");
            }
            domains = domainGenerator.generateSubdomains(baseDomain, subDomains);
        }
        String proxyId = uidGenerator.getUIDAsString();
        config.setProxyId(proxyId);
        proxyManager.activate(config, domains.stream().map(DomainInfo::getFullDomain).collect(Collectors.toSet()));
        logger.debug("HTTP代理 {} 创建成功，域名: {}", config.getName(), domains);
        return new ProxyOperationResult(domains, null, true);
    }

    @Override
    public ProxyOperationResult update(ProxyConfig newConfig, ProxyConfig oldConfig, AgentInfo agentInfo) {
        if (oldConfig.getProtocol() != newConfig.getProtocol()) {
            logger.debug("HTTP代理更新 {} 协议类型发生变化，旧: {}, 新: {}",
                    newConfig.getName(), oldConfig.getProtocol().name(), newConfig.getProtocol().name());
        }
        logger.debug("更新HTTP代理: {}", newConfig.getName());
        proxyManager.deactivate(oldConfig.getProxyId());
        return create(newConfig, agentInfo);
    }

    @Override
    public boolean supports(ProxyConfig config) {
        return config.isHttp();
    }
}
