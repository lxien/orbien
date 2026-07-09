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


import io.github.lxien.orbien.server.service.ProxyRuntimeRegistry;
import io.github.lxien.orbien.server.statemachine.agent.AgentContext;
import io.github.lxien.orbien.server.statemachine.agent.AgentState;
import io.github.lxien.orbien.server.statemachine.agent.AgentEvent;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 客户端认证/重连后，将该客户端下已启用的代理注册到运行时。
 */
@Component
public class AgentInitAction extends AgentBaseAction {
    private final InternalLogger logger = InternalLoggerFactory.getInstance(AgentInitAction.class);
    @Autowired
    private ProxyRuntimeRegistry proxyRuntimeRegistry;

    @Override
    protected void doExecute(AgentState from, AgentState to, AgentEvent event, AgentContext context) {
        logger.debug("初始化客户端运行时配置信息");
        proxyRuntimeRegistry.registerByAgentId(context.getAgentInfo().getAgentId());
    }
}
