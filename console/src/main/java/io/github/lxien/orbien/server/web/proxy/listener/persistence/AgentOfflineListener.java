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

import io.github.lxien.orbien.core.enums.AgentType;
import io.github.lxien.orbien.server.event.AgentOfflineEvent;
import io.github.lxien.orbien.server.web.entity.AgentDO;
import io.github.lxien.orbien.server.event.ProxyDeleteEvent;
import io.github.lxien.orbien.server.notify.EventBus;
import io.github.lxien.orbien.server.notify.EventListener;
import io.github.lxien.orbien.server.service.AgentConfigService;
import io.github.lxien.orbien.server.service.ProxyCacheEvictionService;
import io.github.lxien.orbien.server.web.entity.ProxyDO;
import io.github.lxien.orbien.server.web.repository.AgentRepository;
import io.github.lxien.orbien.server.web.repository.ProxyRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;


/**
 * 会话型客户端离线后，清理 agent 及关联代理的数据库记录。
 */
@Component
public class AgentOfflineListener implements EventListener<AgentOfflineEvent> {
    private final Logger logger = LoggerFactory.getLogger(AgentOfflineListener.class);

    @Autowired
    private EventBus eventBus;
    @Autowired
    private ProxyRepository proxyRepository;
    @Autowired
    private AgentRepository agentRepository;
    @Autowired
    private AgentConfigService agentConfigService;
    @Autowired
    private ProxyCacheEvictionService proxyCacheEvictionService;
    @Autowired
    private TransactionTemplate transactionTemplate;

    @PostConstruct
    public void init() {
        eventBus.register(this);
    }
    @Override
    public void onEvent(AgentOfflineEvent event) {
        if (event == null || !StringUtils.hasText(event.getAgentId())) {
            return;
        }
        String agentId = event.getAgentId();
        AgentDO agent = agentRepository.findById(agentId).orElse(null);
        if (agent == null) {
            return;
        }
        AgentType agentType = agent.getAgentType();
        if (agentType == null || !agentType.isSession()) {
            return;
        }
        transactionTemplate.executeWithoutResult(status -> {
            try {
                deleteSessionAgent(agentId);
            } catch (Exception e) {
                status.setRollbackOnly();
                logger.error("会话型客户端数据库记录清理失败: agentId={}", agentId, e);
            }
        });
    }

    private void deleteSessionAgent(String agentId) {
        List<ProxyDO> proxies = proxyRepository.findByAgentId(agentId);
        List<String> proxyIds = new ArrayList<>();
        if (!CollectionUtils.isEmpty(proxies)) {
            for (ProxyDO proxy : proxies) {
                proxyIds.add(proxy.getId());
                proxyCacheEvictionService.evictByProxyId(proxy.getId());
                eventBus.publishSync(new ProxyDeleteEvent(agentId, proxy.getId()));
            }
        }
        if (agentRepository.existsById(agentId)) {
            agentRepository.deleteById(agentId);
        }
        agentConfigService.evictById(agentId);
        logger.debug("会话型客户端数据库记录已清理: agentId={}, proxyCount={}", agentId, proxyIds.size());
    }
}
