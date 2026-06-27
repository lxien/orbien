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
package com.xiaoniucode.etp.server.statemachine.agent.action;


import com.xiaoniucode.etp.core.domain.ProxyConfig;
import com.xiaoniucode.etp.server.manager.ProxyManager;
import com.xiaoniucode.etp.server.service.ProxyConfigExt;
import com.xiaoniucode.etp.server.service.ProxyConfigService;
import com.xiaoniucode.etp.server.statemachine.agent.AgentContext;
import com.xiaoniucode.etp.server.statemachine.agent.AgentState;
import com.xiaoniucode.etp.server.statemachine.agent.AgentEvent;
import com.xiaoniucode.etp.server.vhost.DomainInfo;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 初始化登陆后的客户端运行时信息
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
                    if (config.isHttp()) {
                        Set<String> domains = configExt.getDomains()
                                .stream()
                                .map(DomainInfo::getFullDomain)
                                .collect(Collectors.toSet());
                        proxyManager.activate(config, domains);
                        logger.debug("激活HTTP代理配置: {}", config.getName());
                    } else {
                        logger.debug("激活TCP代理配置: {}", config.getName());
                        proxyManager.activate(config);
                    }
                }
            });
        }
    }
}
