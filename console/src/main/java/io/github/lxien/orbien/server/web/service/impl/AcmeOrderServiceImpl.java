package io.github.lxien.orbien.server.web.service.impl;

import io.github.lxien.orbien.server.web.common.exception.BizException;
import io.github.lxien.orbien.server.web.common.message.PageQuery;
import io.github.lxien.orbien.server.web.common.message.PageResult;
import io.github.lxien.orbien.server.web.common.utils.JsonUtils;
import io.github.lxien.orbien.server.web.config.AcmeProperties;
import io.github.lxien.orbien.server.web.dto.acme.AcmeDnsChallengeDTO;
import io.github.lxien.orbien.server.web.dto.acme.AcmeOrderDTO;
import io.github.lxien.orbien.server.web.dto.tls.TlsCertDTO;
import io.github.lxien.orbien.server.web.entity.AcmeCertOrderDO;
import io.github.lxien.orbien.server.web.entity.AcmeDnsChallengeDO;
import io.github.lxien.orbien.server.web.entity.DnsCredentialDO;
import io.github.lxien.orbien.server.web.enums.*;
import io.github.lxien.orbien.server.web.enums.AcmeChallengeStatus;
import io.github.lxien.orbien.server.web.enums.AcmeOrderStatus;
import io.github.lxien.orbien.server.web.enums.AcmeValidationMode;
import io.github.lxien.orbien.server.web.param.acme.AcmeOrderCreateParam;
import io.github.lxien.orbien.server.web.param.binding.CertBindParam;
import io.github.lxien.orbien.server.web.repository.AcmeCertOrderRepository;
import io.github.lxien.orbien.server.web.repository.AcmeDnsChallengeRepository;
import io.github.lxien.orbien.server.web.repository.DnsCredentialRepository;
import io.github.lxien.orbien.server.web.service.AcmeOrderService;
import io.github.lxien.orbien.server.web.service.CertBindingService;
import io.github.lxien.orbien.server.web.service.DnsCredentialService;
import io.github.lxien.orbien.server.web.service.TlsCertificateService;
import io.github.lxien.orbien.server.web.service.acme.AcmeClientService;
import io.github.lxien.orbien.server.web.service.acme.DnsNameHelper;
import io.github.lxien.orbien.server.web.service.acme.DnsPropagationChecker;
import io.github.lxien.orbien.server.web.service.dns.DnsProviderAdapter;
import io.github.lxien.orbien.server.web.service.dns.DnsProviderConfig;
import io.github.lxien.orbien.server.web.service.dns.DnsProviderRegistry;
import io.github.lxien.orbien.server.web.service.dns.DnsRecordRef;
import lombok.RequiredArgsConstructor;
import org.shredzone.acme4j.Login;
import org.shredzone.acme4j.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class AcmeOrderServiceImpl implements AcmeOrderService {

    private static final Logger logger = LoggerFactory.getLogger(AcmeOrderServiceImpl.class);
    private static final Pattern DOMAIN_PATTERN = Pattern.compile("^([a-z0-9]([a-z0-9-]{0,61}[a-z0-9])?\\.)+[a-z]{2,}$");
    private static final Pattern WILDCARD_PATTERN = Pattern.compile("^\\*\\.([a-z0-9]([a-z0-9-]{0,61}[a-z0-9])?\\.)+[a-z]{2,}$");

    private final AcmeCertOrderRepository acmeCertOrderRepository;
    private final AcmeDnsChallengeRepository acmeDnsChallengeRepository;
    private final DnsCredentialRepository dnsCredentialRepository;
    private final DnsCredentialService dnsCredentialService;
    private final AcmeClientService acmeClientService;
    private final DnsProviderRegistry dnsProviderRegistry;
    private final DnsPropagationChecker dnsPropagationChecker;
    private final TlsCertificateService tlsCertificateService;
    private final CertBindingService certBindingService;
    private final AcmeProperties acmeProperties;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AcmeOrderDTO createAndSubmit(AcmeOrderCreateParam param) {
        List<String> domains = normalizeDomains(param.getDomains());
        AcmeValidationMode validationMode = AcmeValidationMode.fromCode(param.getValidationMode());
        if (validationMode == AcmeValidationMode.DNS_API && param.getDnsCredentialId() == null) {
            throw new BizException("云DNS自动解析需要选择DNS密钥");
        }

        AcmeCertOrderDO order = new AcmeCertOrderDO();
        order.setOrderNo(generateOrderNo());
        order.setStatus(AcmeOrderStatus.DRAFT);
        order.setDomains(JsonUtils.toJson(domains));
        order.setValidationMode(validationMode);
        order.setAutoRenew(Boolean.TRUE.equals(param.getAutoRenew()));
        if (!CollectionUtils.isEmpty(param.getBindProxyDomainIds())) {
            order.setBindProxyDomainIds(JsonUtils.toJson(param.getBindProxyDomainIds()));
        }
        if (validationMode == AcmeValidationMode.DNS_API) {
            DnsCredentialDO credential = dnsCredentialRepository.findById(param.getDnsCredentialId())
                    .orElseThrow(() -> new BizException("DNS密钥不存在"));
            order.setDnsCredentialId(credential.getId());
            order.setDnsProvider(credential.getProvider());
        }
        order = acmeCertOrderRepository.save(order);
        submitOrder(order.getId());
        return getDetail(order.getId());
    }

    @Override
    public AcmeOrderDTO getDetail(Long orderId) {
        AcmeCertOrderDO order = requireOrder(orderId);
        List<AcmeDnsChallengeDO> challenges = acmeDnsChallengeRepository.findByOrderId(orderId);
        return toDTO(order, challenges);
    }

    @Override
    public PageResult<AcmeOrderDTO> findByPage(PageQuery pageQuery) {
        int currentPage = Math.max(0, pageQuery.getCurrent() - 1);
        Pageable pageable = PageRequest.of(currentPage, pageQuery.getSize(), Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<AcmeCertOrderDO> page = acmeCertOrderRepository.findAllByOrderByCreatedAtDesc(pageable);
        if (page.isEmpty()) {
            return PageResult.empty(pageQuery.getCurrent(), pageQuery.getSize());
        }
        List<AcmeOrderDTO> records = page.getContent().stream()
                .map(order -> toDTO(order, acmeDnsChallengeRepository.findByOrderId(order.getId())))
                .toList();
        return PageResult.wrap(page, records);
    }

    @Override
    public void verify(Long orderId) {
        AcmeCertOrderDO order = requireOrder(orderId);
        if (order.getStatus().isTerminal()) {
            throw new BizException("申请已结束，无法继续验证");
        }
        if (order.getStatus() == AcmeOrderStatus.VALIDATING || order.getStatus() == AcmeOrderStatus.ISSUING) {
            return;
        }
        scheduleVerification(orderId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long orderId) {
        AcmeCertOrderDO order = requireOrder(orderId);
        if (order.getStatus() == AcmeOrderStatus.SUCCESS) {
            throw new BizException("已完成的申请无法取消");
        }
        cleanupDnsRecords(orderId);
        order.setStatus(AcmeOrderStatus.CANCELLED);
        acmeCertOrderRepository.save(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void retry(Long orderId) {
        AcmeCertOrderDO order = requireOrder(orderId);
        if (order.getStatus() != AcmeOrderStatus.FAILED) {
            throw new BizException("仅失败订单可重试");
        }
        cleanupDnsRecords(orderId);
        acmeDnsChallengeRepository.deleteByOrderId(orderId);
        order.setStatus(AcmeOrderStatus.DRAFT);
        order.setErrorCode(null);
        order.setErrorMessage(null);
        acmeCertOrderRepository.save(order);
        submitOrder(orderId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteByIds(List<Long> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            throw new BizException("请选择要删除的申请记录");
        }
        for (Long orderId : ids) {
            acmeCertOrderRepository.findById(orderId).ifPresent(order -> {
                cleanupDnsRecords(orderId);
                acmeDnsChallengeRepository.deleteByOrderId(orderId);
                acmeCertOrderRepository.delete(order);
            });
        }
    }

    private void scheduleVerification(Long orderId) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    CompletableFuture.runAsync(() -> processVerification(orderId));
                }
            });
        } else {
            CompletableFuture.runAsync(() -> processVerification(orderId));
        }
    }

    private void submitOrder(Long orderId) {
        AcmeCertOrderDO order = requireOrder(orderId);
        List<String> domains = JsonUtils.toStringList(order.getDomains());
        try {
            Session session = acmeClientService.newSession();
            Login login = acmeClientService.loadOrCreateLogin(session);
            AcmeClientService.PreparedOrder preparedOrder = acmeClientService.prepareOrder(login, domains);
            order.setAcmeOrderUrl(preparedOrder.getOrderUrl());
            order.setStatus(AcmeOrderStatus.PENDING_DNS);
            acmeCertOrderRepository.save(order);

            acmeDnsChallengeRepository.deleteByOrderId(orderId);
            DnsProviderAdapter adapter = null;
            DnsProviderConfig config = null;
            if (order.getValidationMode() == AcmeValidationMode.DNS_API) {
                config = dnsCredentialService.resolveConfig(order.getDnsCredentialId());
                adapter = dnsProviderRegistry.get(order.getDnsProvider());
            }

            for (AcmeClientService.PreparedChallenge challenge : preparedOrder.getChallenges()) {
                AcmeDnsChallengeDO challengeDO = new AcmeDnsChallengeDO();
                challengeDO.setOrderId(orderId);
                challengeDO.setDomain(challenge.getDomain());
                challengeDO.setRecordName(challenge.getRecordName());
                challengeDO.setRecordValue(challenge.getRecordValue());
                challengeDO.setChallengeUrl(challenge.getChallengeUrl());
                challengeDO.setStatus(AcmeChallengeStatus.PENDING);

                String zoneName = adapter != null
                        ? adapter.resolveZone(challenge.getDomain(), config)
                        : DnsNameHelper.guessZone(challenge.getDomain());
                challengeDO.setDnsZone(zoneName);
                challengeDO.setHostRecord(DnsNameHelper.buildHostRecord(challenge.getRecordName(), zoneName));

                if (adapter != null) {
                    DnsRecordRef recordRef = adapter.addTxtRecord(
                            challenge.getDomain(),
                            challenge.getRecordName(),
                            challenge.getRecordValue(),
                            config);
                    challengeDO.setProviderRecordId(recordRef.providerRecordId());
                    challengeDO.setProviderZone(recordRef.zone());
                    challengeDO.setStatus(AcmeChallengeStatus.PROPAGATING);
                }
                acmeDnsChallengeRepository.save(challengeDO);
            }
            order.setStatus(AcmeOrderStatus.DNS_WAITING);
            acmeCertOrderRepository.save(order);
            if (order.getValidationMode() == AcmeValidationMode.DNS_API) {
                scheduleVerification(orderId);
            }
        } catch (Exception e) {
            markFailed(order, e.getMessage());
            throw new BizException("提交申请失败: " + e.getMessage());
        }
    }

    private void processVerification(Long orderId) {
        try {
            AcmeCertOrderDO order = requireOrder(orderId);
            if (order.getStatus().isTerminal() || order.getStatus() == AcmeOrderStatus.CANCELLED) {
                return;
            }
            order.setStatus(AcmeOrderStatus.VALIDATING);
            acmeCertOrderRepository.save(order);

            PropagationWaitResult propagationResult = waitForDnsPropagation(orderId, order.getValidationMode());
            if (!propagationResult.success()) {
                AcmeCertOrderDO latest = requireOrder(orderId);
                if (latest.getStatus() != AcmeOrderStatus.CANCELLED) {
                    markFailed(latest, propagationResult.errorMessage());
                }
                return;
            }

            order = requireOrder(orderId);
            if (order.getStatus().isTerminal() || order.getStatus() == AcmeOrderStatus.CANCELLED) {
                return;
            }

            Session session = acmeClientService.newSession();
            Login login = acmeClientService.loadOrCreateLogin(session);
            acmeClientService.triggerChallenges(login, order.getAcmeOrderUrl());
            boolean validated = acmeClientService.pollAuthorizations(
                    login,
                    order.getAcmeOrderUrl(),
                    acmeProperties.getDnsPollMaxAttempts(),
                    acmeProperties.getDnsPollIntervalSeconds());
            if (!validated) {
                markFailed(order, "CA 验证超时，请稍后重试");
                return;
            }

            order.setStatus(AcmeOrderStatus.ISSUING);
            acmeCertOrderRepository.save(order);
            List<String> domains = JsonUtils.toStringList(order.getDomains());
            AcmeClientService.IssuedCertificate issued = acmeClientService.issueCertificate(login, order.getAcmeOrderUrl());
            TlsCertDTO cert = tlsCertificateService.saveAcmeCert(issued.getKeyPem(), issued.getFullChainPem());

            List<Long> bindDomainIds = JsonUtils.toLongList(order.getBindProxyDomainIds());
            if (!CollectionUtils.isEmpty(bindDomainIds)) {
                CertBindParam bindParam = new CertBindParam();
                bindParam.setCertId(cert.getId());
                bindParam.setProxyDomainIds(bindDomainIds);
                bindParam.setOverride(true);
                certBindingService.bind(bindParam);
            }

            cleanupDnsRecords(orderId);
            order.setCertId(cert.getId());
            order.setStatus(AcmeOrderStatus.SUCCESS);
            order.setExpiresAt(cert.getNotAfter() != null ? cert.getNotAfter().atStartOfDay() : null);
            order.setErrorCode(null);
            order.setErrorMessage(null);
            acmeCertOrderRepository.save(order);
            if (Boolean.TRUE.equals(order.getAutoRenew())) {
                tlsCertificateService.updateAutoRenew(cert.getId(), true);
            }
        } catch (Exception e) {
            logger.error("ACME 申请验证失败: orderId={}", orderId, e);
            acmeCertOrderRepository.findById(orderId).ifPresent(order -> {
                markFailed(order, resolveErrorMessage(e));
                cleanupDnsRecords(orderId);
            });
        }
    }

    private String resolveErrorMessage(Exception e) {
        if (e instanceof BizException bizException) {
            return bizException.getMessage();
        }
        return e.getMessage() != null ? e.getMessage() : "ACME 验证失败";
    }

    private record PropagationWaitResult(boolean success, String errorMessage) {
        static PropagationWaitResult ok() {
            return new PropagationWaitResult(true, null);
        }

        static PropagationWaitResult fail(String errorMessage) {
            return new PropagationWaitResult(false, errorMessage);
        }
    }

    private PropagationWaitResult waitForDnsPropagation(Long orderId, AcmeValidationMode validationMode) {
        int maxAttempts = Math.max(1, acmeProperties.getDnsPropagationMaxAttempts());
        int failFastAttempts = Math.max(1, acmeProperties.getDnsPropagationFailFastAttempts());
        int consecutiveUnavailable = 0;

        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            AcmeCertOrderDO order = acmeCertOrderRepository.findById(orderId).orElse(null);
            if (order == null || order.getStatus().isTerminal() || order.getStatus() == AcmeOrderStatus.CANCELLED) {
                return PropagationWaitResult.fail("申请已取消或已结束");
            }

            List<AcmeDnsChallengeDO> challenges = acmeDnsChallengeRepository.findByOrderId(orderId);
            if (CollectionUtils.isEmpty(challenges)) {
                return PropagationWaitResult.fail("未找到 DNS 验证记录");
            }

            boolean allReady = true;
            boolean anyUnavailable = false;
            for (AcmeDnsChallengeDO challenge : challenges) {
                if (challenge.getStatus() == AcmeChallengeStatus.VALIDATED) {
                    continue;
                }
                DnsPropagationChecker.TxtCheckStatus checkStatus = dnsPropagationChecker.checkTxt(
                        challenge.getRecordName(),
                        challenge.getRecordValue());
                if (checkStatus == DnsPropagationChecker.TxtCheckStatus.MATCHED) {
                    if (challenge.getStatus() != AcmeChallengeStatus.VALIDATED) {
                        challenge.setStatus(AcmeChallengeStatus.VALIDATED);
                        challenge.setValidatedAt(LocalDateTime.now());
                        acmeDnsChallengeRepository.save(challenge);
                    }
                } else {
                    allReady = false;
                    if (checkStatus == DnsPropagationChecker.TxtCheckStatus.LOOKUP_UNAVAILABLE) {
                        anyUnavailable = true;
                    }
                    AcmeChallengeStatus waitingStatus = validationMode == AcmeValidationMode.DNS_API
                            ? AcmeChallengeStatus.PROPAGATING
                            : AcmeChallengeStatus.PENDING;
                    updateChallengeStatus(challenge, waitingStatus);
                }
            }
            if (allReady) {
                return PropagationWaitResult.ok();
            }

            if (anyUnavailable) {
                consecutiveUnavailable++;
                if (consecutiveUnavailable >= failFastAttempts) {
                    return PropagationWaitResult.fail("域名 DNS 无法解析，请检查域名是否正确或 TXT 记录是否已配置");
                }
            } else {
                consecutiveUnavailable = 0;
            }

            if (attempt < maxAttempts - 1) {
                sleepInterval();
            }
        }
        return PropagationWaitResult.fail("DNS 记录在超时时间内未生效，请检查 TXT 记录后重试");
    }

    private void updateChallengeStatus(AcmeDnsChallengeDO challenge, AcmeChallengeStatus status) {
        if (challenge.getStatus() == status) {
            return;
        }
        challenge.setStatus(status);
        acmeDnsChallengeRepository.save(challenge);
    }

    private void sleepInterval() {
        try {
            Thread.sleep(acmeProperties.getDnsPollIntervalSeconds() * 1000L);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void cleanupDnsRecords(Long orderId) {
        AcmeCertOrderDO order = acmeCertOrderRepository.findById(orderId).orElse(null);
        if (order == null
                || order.getValidationMode() != AcmeValidationMode.DNS_API
                || order.getDnsCredentialId() == null) {
            return;
        }
        try {
            DnsProviderConfig config = dnsCredentialService.resolveConfig(order.getDnsCredentialId());
            DnsProviderAdapter adapter = dnsProviderRegistry.get(order.getDnsProvider());
            for (AcmeDnsChallengeDO challenge : acmeDnsChallengeRepository.findByOrderId(orderId)) {
                if (!StringUtils.hasText(challenge.getProviderRecordId())) {
                    continue;
                }
                if (challenge.getStatus() == AcmeChallengeStatus.CLEANED) {
                    continue;
                }
                try {
                    String providerZone = StringUtils.hasText(challenge.getProviderZone())
                            ? challenge.getProviderZone()
                            : adapter.resolveZone(challenge.getDomain(), config);
                    adapter.removeTxtRecord(new DnsRecordRef(challenge.getProviderRecordId(), providerZone), config);
                    challenge.setStatus(AcmeChallengeStatus.CLEANED);
                    acmeDnsChallengeRepository.save(challenge);
                } catch (Exception e) {
                    logger.warn(
                            "清理 DNS TXT 记录失败: orderId={}, challengeId={}, recordId={}",
                            orderId,
                            challenge.getId(),
                            challenge.getProviderRecordId(),
                            e);
                }
            }
        } catch (Exception e) {
            logger.warn("清理 DNS TXT 记录失败: orderId={}", orderId, e);
        }
    }

    private void markFailed(AcmeCertOrderDO order, String message) {
        order.setStatus(AcmeOrderStatus.FAILED);
        order.setErrorCode("ACME_FAILED");
        order.setErrorMessage(message);
        acmeCertOrderRepository.save(order);
        markChallengesFailed(order.getId());
    }

    private void markChallengesFailed(Long orderId) {
        for (AcmeDnsChallengeDO challenge : acmeDnsChallengeRepository.findByOrderId(orderId)) {
            if (challenge.getStatus() == AcmeChallengeStatus.VALIDATED
                    || challenge.getStatus() == AcmeChallengeStatus.CLEANED) {
                continue;
            }
            updateChallengeStatus(challenge, AcmeChallengeStatus.FAILED);
        }
    }

    private AcmeCertOrderDO requireOrder(Long orderId) {
        return acmeCertOrderRepository.findById(orderId).orElseThrow(() -> new BizException("申请记录不存在"));
    }

    private List<String> normalizeDomains(List<String> domains) {
        if (CollectionUtils.isEmpty(domains)) {
            throw new BizException("请至少填写一个域名");
        }
        Set<String> normalized = new LinkedHashSet<>();
        for (String domain : domains) {
            if (!StringUtils.hasText(domain)) {
                continue;
            }
            String value = domain.trim().toLowerCase(Locale.ROOT);
            if (value.endsWith(".")) {
                value = value.substring(0, value.length() - 1);
            }
            if (!isValidDomain(value)) {
                throw new BizException("域名格式不正确: " + domain);
            }
            normalized.add(value);
        }
        if (normalized.isEmpty()) {
            throw new BizException("请至少填写一个有效域名");
        }
        if (normalized.size() > 100) {
            throw new BizException("单次最多申请 100 个域名");
        }
        return new ArrayList<>(normalized);
    }

    private boolean isValidDomain(String domain) {
        if (DnsNameHelper.looksLikeWildcard(domain)) {
            return WILDCARD_PATTERN.matcher(domain).matches();
        }
        return DOMAIN_PATTERN.matcher(domain).matches();
    }

    private String generateOrderNo() {
        return "ACME" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
    }

    private AcmeOrderDTO toDTO(AcmeCertOrderDO order, List<AcmeDnsChallengeDO> challenges) {
        AcmeOrderDTO dto = new AcmeOrderDTO();
        dto.setId(order.getId());
        dto.setOrderNo(order.getOrderNo());
        dto.setStatus(order.getStatus().getCode());
        dto.setStatusLabel(order.getStatus().getLabel());
        dto.setDomains(JsonUtils.toStringList(order.getDomains()));
        dto.setValidationMode(order.getValidationMode().getCode());
        dto.setDnsCredentialId(order.getDnsCredentialId());
        dto.setDnsProvider(order.getDnsProvider() != null ? order.getDnsProvider().getCode() : null);
        dto.setCertId(order.getCertId());
        dto.setBindProxyDomainIds(JsonUtils.toLongList(order.getBindProxyDomainIds()));
        dto.setErrorCode(order.getErrorCode());
        dto.setErrorMessage(order.getErrorMessage());
        dto.setExpiresAt(order.getExpiresAt());
        dto.setCreatedAt(order.getCreatedAt());
        if (!CollectionUtils.isEmpty(challenges)) {
            dto.setChallenges(challenges.stream().map(this::toChallengeDTO).toList());
        }
        return dto;
    }

    private AcmeDnsChallengeDTO toChallengeDTO(AcmeDnsChallengeDO challenge) {
        AcmeDnsChallengeDTO dto = new AcmeDnsChallengeDTO();
        dto.setId(challenge.getId());
        dto.setDomain(challenge.getDomain());
        dto.setRecordName(challenge.getRecordName());
        dto.setHostRecord(StringUtils.hasText(challenge.getHostRecord())
                ? challenge.getHostRecord()
                : DnsNameHelper.buildHostRecord(
                challenge.getRecordName(),
                StringUtils.hasText(challenge.getDnsZone())
                ? challenge.getDnsZone()
                : DnsNameHelper.guessZone(challenge.getDomain())));
        dto.setDnsZone(StringUtils.hasText(challenge.getDnsZone())
                ? challenge.getDnsZone()
                : DnsNameHelper.guessZone(challenge.getDomain()));
        dto.setRecordValue(challenge.getRecordValue());
        dto.setRecordType(challenge.getRecordType());
        dto.setStatus(challenge.getStatus().getCode());
        dto.setStatusLabel(challenge.getStatus().getLabel());
        return dto;
    }
}
