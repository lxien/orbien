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

import io.github.lxien.orbien.core.domain.DomainInfo;
import io.github.lxien.orbien.core.enums.AccessControl;
import io.github.lxien.orbien.core.enums.HealthCheckType;
import io.github.lxien.orbien.core.enums.ProtocolType;
import io.github.lxien.orbien.core.message.Message;
import io.github.lxien.orbien.core.notify.EventBus;
import io.github.lxien.orbien.core.notify.EventListener;
import io.github.lxien.orbien.server.event.ProxyAddEvent;
import io.github.lxien.orbien.server.web.entity.*;
import io.github.lxien.orbien.server.web.entity.*;
import io.github.lxien.orbien.server.web.proxy.converter.ProxyReportConvert;
import io.github.lxien.orbien.server.web.repository.*;
import io.github.lxien.orbien.server.web.repository.*;
import io.github.lxien.orbien.server.web.service.CertBindingSyncService;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

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
    private Socks5AuthRepository socks5AuthRepository;
    @Autowired
    private Socks5UserRepository socks5UserRepository;
    @Autowired
    private FileShareAuthRepository fileShareAuthRepository;
    @Autowired
    private FileShareUserRepository fileShareUserRepository;
    @Autowired
    private FileShareLimitsRepository fileShareLimitsRepository;
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
        if (protocol.isFile()) {
            applyFileProxyFields(proxyDO, event);
        }

        proxyRepository.save(proxyDO);
        if (!protocol.isSocks5() && !protocol.isFile()) {
            persistTargets(proxyId, proxy);
        }
        persistAccessControl(proxyId, proxy);
        if (!protocol.isSocks5() && !protocol.isFile()) {
            persistHealthCheck(proxyId, proxy);
        }

        if (protocol.isHttpOrHttps()) {
            persistBasicAuth(proxyId, proxy);
            persistDomains(proxyId, event.getDomains());
        }
        if (protocol.isSocks5()) {
            persistSocks5Auth(proxyId, proxy);
        }
        if (protocol.isFile()) {
            persistFileShareAuth(proxyId, proxy);
            persistFileShareLimits(proxyId, proxy);
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

    private void applyFileProxyFields(ProxyDO proxyDO, ProxyAddEvent event) {
        applyHttpProxyFields(proxyDO, event);
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

    private void persistSocks5Auth(String proxyId, Message.Proxy proxy) {
        socks5UserRepository.deleteByProxyIdIn(List.of(proxyId));
        if (proxy.hasSocks5Auth()) {
            Message.Socks5Auth socks5Auth = proxy.getSocks5Auth();
            socks5AuthRepository.save(new Socks5AuthDO(proxyId, socks5Auth.getEnabled()));
            if (!socks5Auth.getUsersList().isEmpty()) {
                List<Socks5UserDO> users = proxyReportConvert.toSocks5UserDOList(socks5Auth, proxyId);
                users.forEach(user -> user.setPassword(passwordEncoder.encode(user.getPassword())));
                socks5UserRepository.saveAll(users);
            }
            return;
        }
        socks5AuthRepository.save(new Socks5AuthDO(proxyId, false));
    }

    private void persistFileShareAuth(String proxyId, Message.Proxy proxy) {
        fileShareUserRepository.deleteByProxyIdIn(List.of(proxyId));
        if (proxy.hasFileAuth()) {
            Message.FileShareAuth fileAuth = proxy.getFileAuth();
            fileShareAuthRepository.save(new FileShareAuthDO(proxyId, fileAuth.getEnabled()));
            if (!fileAuth.getUsersList().isEmpty()) {
                List<FileShareUserDO> users = proxyReportConvert.toFileShareUserDOList(fileAuth, proxyId);
                users.forEach(user -> user.setPassword(passwordEncoder.encode(user.getPassword())));
                fileShareUserRepository.saveAll(users);
            }
            return;
        }
        fileShareAuthRepository.save(new FileShareAuthDO(proxyId, false));
    }

    private void persistFileShareLimits(String proxyId, Message.Proxy proxy) {
        if (!proxy.hasFileLimits()) {
            return;
        }
        Message.FileShareLimits limits = proxy.getFileLimits();
        FileShareLimitsDO limitsDO = new FileShareLimitsDO(proxyId);
        if (StringUtils.hasText(limits.getRootPath())) {
            limitsDO.setRootPath(limits.getRootPath());
        }
        if (limits.getMaxUploadSize() > 0) {
            limitsDO.setMaxUploadSize(limits.getMaxUploadSize());
        }
        limitsDO.setAllowUpload(limits.getAllowUpload());
        limitsDO.setAllowDelete(limits.getAllowDelete());
        limitsDO.setAllowMkdir(limits.getAllowMkdir());
        fileShareLimitsRepository.save(limitsDO);
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
