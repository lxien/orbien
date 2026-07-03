package com.xiaoniucode.etp.server.web.service.impl;

import com.xiaoniucode.etp.server.web.common.utils.JsonUtils;
import com.xiaoniucode.etp.server.web.dto.acme.AcmeOrderDTO;
import com.xiaoniucode.etp.server.web.entity.AcmeCertOrderDO;
import com.xiaoniucode.etp.server.web.entity.SslCertDO;
import com.xiaoniucode.etp.server.web.enums.AcmeOrderStatus;
import com.xiaoniucode.etp.server.web.enums.CertSource;
import com.xiaoniucode.etp.server.web.enums.SslStatus;
import com.xiaoniucode.etp.server.web.param.acme.AcmeOrderCreateParam;
import com.xiaoniucode.etp.server.web.repository.AcmeCertOrderRepository;
import com.xiaoniucode.etp.server.web.repository.SslCertRepository;
import com.xiaoniucode.etp.server.web.service.AcmeOrderService;
import com.xiaoniucode.etp.server.web.service.AcmeRenewService;
import com.xiaoniucode.etp.server.web.service.scheduled.job.AcmeRenewJobParams;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AcmeRenewServiceImpl implements AcmeRenewService {

    private static final Logger logger = LoggerFactory.getLogger(AcmeRenewServiceImpl.class);
    private static final List<AcmeOrderStatus> IN_PROGRESS = List.of(
            AcmeOrderStatus.DRAFT,
            AcmeOrderStatus.PENDING_DNS,
            AcmeOrderStatus.DNS_WAITING,
            AcmeOrderStatus.VALIDATING,
            AcmeOrderStatus.ISSUING);

    private final SslCertRepository sslCertRepository;
    private final AcmeCertOrderRepository acmeCertOrderRepository;
    private final AcmeOrderService acmeOrderService;

    @Override
    public int renewDueCertificates(AcmeRenewJobParams params) {
        LocalDate today = LocalDate.now();
        LocalDate deadline = today.plusDays(params.getRenewBeforeDays());
        List<SslCertDO> candidates = sslCertRepository.findRenewCandidates(deadline, today);
        int renewed = 0;
        for (SslCertDO cert : candidates) {
            if (renewed >= params.getMaxCertsPerRun()) {
                break;
            }
            if (params.isOnlyAcmeSource() && cert.getSource() != CertSource.ACME) {
                continue;
            }
            if (params.isRespectCertAutoRenew() && !Boolean.TRUE.equals(cert.getAutoRenew())) {
                continue;
            }
            if (cert.getStatus() != SslStatus.ACTIVE) {
                continue;
            }
            if (acmeCertOrderRepository.existsByCertIdAndStatusIn(cert.getId(), IN_PROGRESS)) {
                continue;
            }
            try {
                renewSingle(cert);
                renewed++;
            } catch (Exception e) {
                logger.warn("证书续签失败: certId={}, domains={}", cert.getId(), cert.getSanDomains(), e);
            }
        }
        return renewed;
    }

    private void renewSingle(SslCertDO cert) {
        AcmeCertOrderDO template = acmeCertOrderRepository
                .findFirstByCertIdAndStatusOrderByCreatedAtDesc(cert.getId(), AcmeOrderStatus.SUCCESS)
                .orElseThrow(() -> new IllegalStateException("未找到历史成功申请记录"));

        List<String> domains = parseDomains(cert.getSanDomains());
        AcmeOrderCreateParam param = new AcmeOrderCreateParam();
        param.setDomains(domains);
        param.setValidationMode(template.getValidationMode().getCode());
        param.setDnsCredentialId(template.getDnsCredentialId());
        param.setBindProxyDomainIds(JsonUtils.toLongList(template.getBindProxyDomainIds()));
        param.setAutoRenew(true);

        AcmeOrderDTO order = acmeOrderService.createAndSubmit(param);
        cert.setRenewOrderId(order.getId());
        cert.setLastRenewAt(LocalDateTime.now());
        sslCertRepository.save(cert);
    }

    private List<String> parseDomains(String sanDomains) {
        if (!StringUtils.hasText(sanDomains)) {
            throw new IllegalStateException("证书域名为空");
        }
        return Arrays.stream(sanDomains.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toList();
    }
}
