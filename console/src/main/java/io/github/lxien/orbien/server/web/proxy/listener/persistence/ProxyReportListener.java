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
import io.github.lxien.orbien.server.notify.EventBus;
import io.github.lxien.orbien.server.notify.EventListener;
import io.github.lxien.orbien.server.event.ProxyAddEvent;
import io.github.lxien.orbien.server.web.entity.*;
import io.github.lxien.orbien.server.web.proxy.converter.ProxyReportConvert;
import io.github.lxien.orbien.server.web.repository.*;
import io.github.lxien.orbien.server.web.service.CertBindingService;
import io.github.lxien.orbien.server.web.service.CertBindingSyncService;
import io.github.lxien.orbien.server.web.service.TlsCertificateService;
import io.github.lxien.orbien.server.web.dto.tls.TlsCertDTO;
import io.github.lxien.orbien.server.web.enums.CertSource;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashSet;
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
    private HeaderRewriteRepository headerRewriteRepository;
    @Autowired
    private HeaderRewriteRuleRepository headerRewriteRuleRepository;
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
    @Autowired
    private CertBindingService certBindingService;
    @Autowired
    private TlsCertificateService tlsCertificateService;

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
            persistHeaderRewrite(proxyId, proxy);
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

        if (protocol.requiresVisitorTls() && proxy.hasTlsCert()) {
            persistReportedTlsCert(proxyId, proxy);
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
        basicUserRepository.flush();

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

    private void persistHeaderRewrite(String proxyId, Message.Proxy proxy) {
        // Agent TOML 未声明 header_rewrite 时不得清空控制台已配置的规则
        if (!proxy.hasHeaderRewrite()) {
            if (!headerRewriteRepository.existsById(proxyId)) {
                headerRewriteRepository.save(new HeaderRewriteDO(proxyId, false));
            }
            return;
        }

        headerRewriteRuleRepository.deleteByProxyIdIn(List.of(proxyId));
        headerRewriteRuleRepository.flush();

        Message.HeaderRewrite headerRewrite = proxy.getHeaderRewrite();
        headerRewriteRepository.save(new HeaderRewriteDO(proxyId, headerRewrite.getEnabled()));
        if (headerRewrite.getRulesList().isEmpty()) {
            return;
        }
        List<HeaderRewriteRuleDO> rules = new ArrayList<>();
        for (Message.HeaderRewriteRule rule : headerRewrite.getRulesList()) {
            HeaderRewriteRuleDO ruleDO = new HeaderRewriteRuleDO();
            ruleDO.setProxyId(proxyId);
            ruleDO.setDirection(proxyReportConvert.toHeaderDirection(rule.getDirection()));
            ruleDO.setAction(proxyReportConvert.toHeaderAction(rule.getAction()));
            ruleDO.setName(rule.getName());
            ruleDO.setValue(rule.getValue().isEmpty() ? null : rule.getValue());
            io.github.lxien.orbien.core.http.HeaderRewriteSupport.validateRule(
                    new io.github.lxien.orbien.core.domain.HeaderRewriteRule(
                            ruleDO.getAction(), ruleDO.getName(), ruleDO.getValue()));
            rules.add(ruleDO);
        }
        if (rules.size() > io.github.lxien.orbien.core.http.HeaderRewriteSupport.MAX_RULES) {
            throw new IllegalArgumentException("header_rewrite 规则超过限制");
        }
        headerRewriteRuleRepository.saveAll(rules);
    }

    private void persistSocks5Auth(String proxyId, Message.Proxy proxy) {
        socks5UserRepository.deleteByProxyIdIn(List.of(proxyId));
        socks5UserRepository.flush();

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
        fileShareAuthRepository.deleteByProxyIdIn(List.of(proxyId));
        fileShareUserRepository.deleteByProxyIdIn(List.of(proxyId));
        fileShareUserRepository.flush();

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
            fileShareLimitsRepository.deleteByProxyIdIn(List.of(proxyId));
            return;
        }
        Message.FileShareLimits limits = proxy.getFileLimits();
        FileShareLimitsDO limitsDO = fileShareLimitsRepository.findById(proxyId)
                .orElse(new FileShareLimitsDO(proxyId));
        if (StringUtils.hasText(limits.getRootPath())) {
            limitsDO.setRootPath(limits.getRootPath());
        }
        if (limits.getMaxUploadSize() > 0) {
            limitsDO.setMaxUploadSize(limits.getMaxUploadSize());
        }
        limitsDO.setAllowUpload(limits.getAllowUpload());
        limitsDO.setAllowDelete(limits.getAllowDelete());
        limitsDO.setAllowMkdir(limits.getAllowMkdir());
        limitsDO.setAllowMove(limits.getAllowMove());
        limitsDO.setAllowRename(limits.getAllowRename());
        fileShareLimitsRepository.save(limitsDO);
    }

    private void persistDomains(String proxyId, List<DomainInfo> domains) {
        List<ProxyDomainDO> existing = proxyDomainRepository.findByProxyId(proxyId);
        Set<String> incoming = CollectionUtils.isEmpty(domains)
                ? Set.of()
                : domains.stream().map(DomainInfo::getFullDomain).collect(Collectors.toSet());
        Set<String> current = existing.stream()
                .map(ProxyDomainDO::getFullDomain)
                .collect(Collectors.toSet());

        List<Long> removedIds = existing.stream()
                .filter(row -> !incoming.contains(row.getFullDomain()))
                .map(ProxyDomainDO::getId)
                .toList();
        if (!removedIds.isEmpty()) {
            certBindingSyncService.removeBindingsByProxyDomainIds(removedIds);
            proxyDomainRepository.deleteAllById(removedIds);
        }

        if (CollectionUtils.isEmpty(domains)) {
            return;
        }

        Set<String> toAdd = new HashSet<>(incoming);
        toAdd.removeAll(current);
        if (toAdd.isEmpty()) {
            return;
        }

        List<ProxyDomainDO> rows = new ArrayList<>();
        for (DomainInfo domainInfo : domains) {
            if (!toAdd.contains(domainInfo.getFullDomain())) {
                continue;
            }
            rows.add(new ProxyDomainDO(
                    proxyId,
                    domainInfo.getDomain(),
                    domainInfo.getRootDomain(),
                    domainInfo.getDomainType()));
        }
        if (!rows.isEmpty()) {
            proxyDomainRepository.saveAll(rows);
        }
    }

    private void persistReportedTlsCert(String proxyId, Message.Proxy proxy) {
        Message.TlsCert tlsCert = proxy.getTlsCert();
        if (!StringUtils.hasText(tlsCert.getPrivateKeyPem()) || !StringUtils.hasText(tlsCert.getCertChainPem())) {
            return;
        }
        try {
            TlsCertDTO cert = tlsCertificateService.saveOrGetCert(
                    tlsCert.getPrivateKeyPem(), tlsCert.getCertChainPem(), CertSource.AGENT);
            certBindingService.bindMatchingDomainsForProxy(cert.getId(), proxyId, true);
        } catch (Exception e) {
            logger.warn("客户端上报 TLS 证书处理失败: proxyId={}, name={}", proxyId, proxy.getName(), e);
        }
    }
}
