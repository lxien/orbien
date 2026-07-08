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
package io.github.lxien.orbien.server.statemachine.agent.action;


import io.github.lxien.orbien.core.domain.ProxyConfig;
import io.github.lxien.orbien.server.manager.ProxyManager;
import io.github.lxien.orbien.core.domain.ProxyConfigExt;
import io.github.lxien.orbien.server.service.ProxyConfigService;
import io.github.lxien.orbien.server.statemachine.agent.AgentContext;
import io.github.lxien.orbien.server.statemachine.agent.AgentState;
import io.github.lxien.orbien.server.statemachine.agent.AgentEvent;
import io.github.lxien.orbien.core.domain.DomainInfo;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * todo 改造 初始化登陆后的客户端运行时信息
 */
@Component
public class AgentInitAction extends AgentBaseAction {
    private final InternalLogger logger = InternalLoggerFactory.getInstance(AgentInitAction.class);
    @Autowired
    private ProxyConfigService proxyConfigService;
    @Autowired
    private ProxyManager proxyManager;

    @Override
    protected void doExecute(AgentState from, AgentState to, AgentEvent event, AgentContext context) {
        logger.debug("初始化客户端配置信息");
        List<ProxyConfigExt> configs = proxyConfigService.findByAgentId(context.getAgentInfo().getAgentId());
        if (!CollectionUtils.isEmpty(configs)) {
            configs.forEach(configExt -> {
                ProxyConfig config = configExt.getProxyConfig();
                if (config.getStatus().isOpen()) {
                    if (config.isHttpOrHttps()) {
                        Set<String> domains = configExt.getDomains().stream()
                                .map(DomainInfo::getFullDomain).collect(Collectors.toSet());
                        if (config.isHttp()) {
                            proxyManager.registerHttp(config.getAgentId(), config.getProxyId(), domains);
                            logger.debug("注册HTTP代理: {}", config.getName());
                        } else {
                            proxyManager.registerHttps(config.getAgentId(), config.getProxyId(), domains);
                            logger.debug("注册HTTPS代理: {}", config.getName());
                        }
                    } else if (config.isUdp()) {
                        proxyManager.registerUdp(config.getAgentId(), config.getProxyId(), config.getListenPort());
                        logger.debug("注册UDP代理: {}", config.getName());
                    } else if (config.isTcp()) {
                        proxyManager.registerTcp(config.getAgentId(), config.getProxyId(), config.getListenPort());
                        logger.debug("注册TCP代理: {}", config.getName());
                    } else if (config.isSocks5()) {
                        proxyManager.registerSocks5(config.getAgentId(), config.getProxyId(), config.getListenPort());
                        logger.debug("注册SOCKS5代理: {}", config.getName());
                    }
                }
            });
        }
    }
}
