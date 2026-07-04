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

package io.github.lxien.orbien.server.web.proxy.listener.persistence;

import io.github.lxien.orbien.core.notify.EventBus;
import io.github.lxien.orbien.core.notify.EventListener;
import io.github.lxien.orbien.server.event.AgentAuthEvent;
import io.github.lxien.orbien.server.statemachine.agent.AgentInfo;
import io.github.lxien.orbien.server.web.proxy.converter.AgentModelConvert;
import io.github.lxien.orbien.server.web.entity.AgentDO;
import io.github.lxien.orbien.server.web.repository.AgentRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AgentAuthListener implements EventListener<AgentAuthEvent> {
    private final Logger logger = LoggerFactory.getLogger(AgentAuthListener.class);
    @Autowired
    private EventBus eventBus;
    @Autowired
    private AgentRepository agentRepository;
    @Autowired
    private AgentModelConvert agentModelConvert;

    @PostConstruct
    public void init() {
        eventBus.register(this);
    }

    @Override
    public void onEvent(AgentAuthEvent event) {
        logger.debug("Received AgentAuthEvent: {}", event);
        boolean reconnect = event.isReconnect();
        AgentInfo agentInfo = event.getAgentInfo();
        if (!reconnect) {
            AgentDO agentDO = agentModelConvert.toDO(agentInfo);
            agentRepository.save(agentDO);
            logger.debug("客户端信息保存成功: agentId={}, name={}",
                    agentInfo.getAgentId(), agentInfo.getName());
        }
    }
}
