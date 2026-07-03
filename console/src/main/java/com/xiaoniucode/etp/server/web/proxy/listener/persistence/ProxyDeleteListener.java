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

package com.xiaoniucode.etp.server.web.proxy.listener.persistence;

import com.xiaoniucode.etp.core.notify.EventBus;
import com.xiaoniucode.etp.core.notify.EventListener;
import com.xiaoniucode.etp.server.event.ProxyDeleteEvent;
import com.xiaoniucode.etp.server.web.entity.ProxyDO;
import com.xiaoniucode.etp.server.web.repository.*;
import com.xiaoniucode.etp.server.web.service.CertBindingSyncService;
import com.xiaoniucode.etp.server.web.service.MetricsService;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Optional;

/**
 * 代理删除事件处理，清理数据库中的代理及其关联配置。
 */
@Component
public class ProxyDeleteListener implements EventListener<ProxyDeleteEvent> {
    private final Logger logger = LoggerFactory.getLogger(ProxyDeleteListener.class);

    @Autowired
    private EventBus eventBus;
    @Autowired
    private ProxyRepository proxyRepository;
    @Autowired
    private ProxyTargetRepository proxyTargetRepository;
    @Autowired
    private AccessControlRepository accessControlRepository;
    @Autowired
    private AccessControlRuleRepository accessControlRuleRepository;
    @Autowired
    private ProxyDomainRepository proxyDomainRepository;
    @Autowired
    private BasicAuthRepository basicAuthRepository;
    @Autowired
    private BasicUserRepository basicUserRepository;
    @Autowired
    private HealthCheckRepository healthCheckRepository;
    @Autowired
    private MetricsService metricsService;
    @Autowired
    private CertBindingSyncService certBindingSyncService;
    @Autowired
    private TransactionTemplate transactionTemplate;

    @PostConstruct
    public void init() {
        eventBus.register(this);
    }

    @Override
    public void onEvent(ProxyDeleteEvent event) {
        transactionTemplate.executeWithoutResult(status -> {
            try {
                deleteProxy(event.getProxyId());
            } catch (Exception e) {
                status.setRollbackOnly();
                logger.error("代理配置从数据库删除失败: agentId={}, proxyId={}",
                        event.getAgentId(), event.getProxyId(), e);
            }
        });
    }

    private void deleteProxy(String proxyId) {
        Optional<ProxyDO> optional = proxyRepository.findById(proxyId);
        if (optional.isEmpty()) {
            return;
        }

        ProxyDO proxyDO = optional.get();
        List<String> ids = List.of(proxyId);

        proxyTargetRepository.deleteByProxyIdIn(ids);
        accessControlRepository.deleteByProxyIdIn(ids);
        accessControlRuleRepository.deleteByProxyIdIn(ids);
        healthCheckRepository.deleteByProxyIdIn(ids);

        if (proxyDO.getProtocol().isHttpOrHttps()) {
            if (proxyDO.getProtocol().isHttps()) {
                certBindingSyncService.removeBindingsByProxyId(proxyId);
            }
            proxyDomainRepository.deleteByProxyId(proxyId);
            basicAuthRepository.deleteByProxyIdIn(ids);
            basicUserRepository.deleteByProxyIdIn(ids);
        }

        metricsService.deleteByProxyId(proxyId);
        proxyRepository.deleteById(proxyId);
        logger.debug("代理配置已从数据库删除: agentId={}, proxyId={}, name={}",
                proxyDO.getAgentId(), proxyId, proxyDO.getName());
    }
}
