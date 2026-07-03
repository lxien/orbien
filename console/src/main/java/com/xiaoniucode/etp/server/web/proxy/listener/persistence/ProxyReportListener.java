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

import com.xiaoniucode.etp.core.domain.DomainInfo;
import com.xiaoniucode.etp.core.enums.AccessControl;
import com.xiaoniucode.etp.core.enums.HealthCheckType;
import com.xiaoniucode.etp.core.enums.ProtocolType;
import com.xiaoniucode.etp.core.message.Message;
import com.xiaoniucode.etp.core.notify.EventBus;
import com.xiaoniucode.etp.core.notify.EventListener;
import com.xiaoniucode.etp.server.event.ProxyAddEvent;
import com.xiaoniucode.etp.server.web.entity.*;
import com.xiaoniucode.etp.server.web.proxy.converter.ProxyReportConvert;
import com.xiaoniucode.etp.server.web.repository.*;
import com.xiaoniucode.etp.server.web.service.CertBindingSyncService;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 代理配置创建事件处理，用于持久化客户端上报的代理配置。
 */
@Component
public class ProxyReportListener implements EventListener<ProxyAddEvent> {
    private final Logger logger = LoggerFactory.getLogger(ProxyReportListener.class);

    @Autowired
    private EventBus eventBus;
    @Autowired
    private ProxyRepository proxyRepository;
    @Autowired
    private AccessControlRepository accessControlRepository;
    @Autowired
    private AccessControlRuleRepository accessControlRuleRepository;
    @Autowired
    private ProxyTargetRepository proxyTargetRepository;
    @Autowired
    private ProxyDomainRepository proxyDomainRepository;
    @Autowired
    private BasicAuthRepository basicAuthRepository;
    @Autowired
    private BasicUserRepository basicUserRepository;
    @Autowired
    private HealthCheckRepository healthCheckRepository;
    @Autowired
    private ProxyReportConvert proxyReportConvert;
    @Autowired
    private TransactionTemplate transactionTemplate;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private CertBindingSyncService certBindingSyncService;

    @PostConstruct
    public void init() {
        eventBus.register(this);
    }

    @Override
    public void onEvent(ProxyAddEvent event) {
        transactionTemplate.executeWithoutResult(status -> {
            try {
                persistProxy(event);
            } catch (Exception e) {
                status.setRollbackOnly();
                logger.error("代理配置信息保存到数据库失败: agentId={}, proxyId={}, name={}",
                        event.getAgentId(), event.getProxyId(), event.getProxy().getName(), e);
            }
        });
    }

    private void persistProxy(ProxyAddEvent event) {
        String agentId = event.getAgentId();
        String proxyId = event.getProxyId();
        Message.Proxy proxy = event.getProxy();
        ProtocolType protocol = ProtocolType.fromName(proxy.getProtocol().name());

        ProxyDO proxyDO = proxyReportConvert.toProxyDO(agentId, proxyId, proxy, event);
        if (protocol.isHttpOrHttps()) {
            applyHttpProxyFields(proxyDO, event);
        }

        proxyRepository.save(proxyDO);
        persistTargets(proxyId, proxy);
        persistAccessControl(proxyId, proxy);
        persistHealthCheck(proxyId, proxy);

        if (protocol.isHttpOrHttps()) {
            persistBasicAuth(proxyId, proxy);
            persistDomains(proxyId, event.getDomains());
        }

        logger.debug("代理配置信息已保存到数据库: agentId={}, proxyId={}, name={}, protocol={}",
                agentId, proxyId, proxy.getName(), protocol);
    }

    private void applyHttpProxyFields(ProxyDO proxyDO, ProxyAddEvent event) {
        List<DomainInfo> domains = event.getDomains();
        if (CollectionUtils.isEmpty(domains)) {
            return;
        }
        proxyDO.setDomainType(domains.getFirst().getDomainType());
    }

    private void persistTargets(String proxyId, Message.Proxy proxy) {
        proxyTargetRepository.deleteByProxyId(proxyId);
        List<ProxyTargetDO> targets = proxyReportConvert.toProxyTargetDOList(proxy.getTargetsList(), proxyId);
        if (!targets.isEmpty()) {
            proxyTargetRepository.saveAll(targets);
        }
    }

    private void persistAccessControl(String proxyId, Message.Proxy proxy) {
        accessControlRuleRepository.deleteByProxyIdIn(List.of(proxyId));

        if (proxy.hasAccessControl()) {
            Message.AccessControl accessControl = proxy.getAccessControl();
            accessControlRepository.save(proxyReportConvert.toAccessControlDO(accessControl, proxyId));
            List<AccessControlRuleDO> rules = proxyReportConvert.toAccessControlRuleDOList(accessControl, proxyId);
            if (!rules.isEmpty()) {
                accessControlRuleRepository.saveAll(rules);
            }
            return;
        }

        AccessControlDO accessControlDO = new AccessControlDO(proxyId, AccessControl.DENY);
        accessControlDO.setEnabled(false);
        accessControlRepository.save(accessControlDO);
    }

    private void persistHealthCheck(String proxyId, Message.Proxy proxy) {
        healthCheckRepository.deleteByProxyIdIn(List.of(proxyId));
        if (proxy.hasHealthCheck()) {
            healthCheckRepository.save(proxyReportConvert.toHealthCheckDO(proxy.getHealthCheck(), proxyId));
            return;
        }
        ProtocolType protocol = ProtocolType.fromName(proxy.getProtocol().name());
        HealthCheckType defaultType = protocol.isHttpOrHttps() ? HealthCheckType.HTTP : HealthCheckType.TCP;
        healthCheckRepository.save(HealthCheckDO.createDefault(proxyId, defaultType));
    }

    private void persistBasicAuth(String proxyId, Message.Proxy proxy) {
        basicUserRepository.deleteByProxyIdIn(List.of(proxyId));

        if (proxy.hasBasicAuth()) {
            Message.BasicAuth basicAuth = proxy.getBasicAuth();
            basicAuthRepository.save(new BasicAuthDO(proxyId, basicAuth.getEnabled()));
            if (!basicAuth.getHttpUsersList().isEmpty()) {
                List<BasicUserDO> users = proxyReportConvert.toBasicUserDOList(basicAuth, proxyId);
                users.forEach(user -> user.setPassword(passwordEncoder.encode(user.getPassword())));
                basicUserRepository.saveAll(users);
            }
            return;
        }

        basicAuthRepository.save(new BasicAuthDO(proxyId, false));
    }

    private void persistDomains(String proxyId, List<DomainInfo> domains) {
        certBindingSyncService.removeBindingsByProxyId(proxyId);
        proxyDomainRepository.deleteByProxyId(proxyId);
        if (CollectionUtils.isEmpty(domains)) {
            return;
        }

        Set<ProxyDomainDO> proxyDomainDOS = domains.stream()
                .map(domainInfo -> new ProxyDomainDO(
                        proxyId,
                        domainInfo.getDomain(),
                        domainInfo.getRootDomain(),
                        domainInfo.getDomainType()))
                .collect(Collectors.toSet());
        proxyDomainRepository.saveAll(proxyDomainDOS);
    }
}
