/*
 *
 *  *    Copyright 2026 xiaoniucode
 *  *
 *  *    Licensed under the Apache License, Version 2.0 (the "License");
 *  *    you may not use this file except in compliance with the License.
 *  *    You may obtain a copy of the License at
 *  *
 *  *        http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  *    Unless required by applicable law or agreed to in writing, software
 *  *    distributed under the License is distributed on an "AS IS" BASIS,
 *  *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  *    See the License for the specific language governing permissions and
 *  *    limitations under the License.
 *
 */

package com.xiaoniucode.etp.server.web.service.impl;

import com.xiaoniucode.etp.core.enums.ProtocolType;
import com.xiaoniucode.etp.server.transport.https.SslCertificateManager;
import com.xiaoniucode.etp.server.web.common.exception.BizException;
import com.xiaoniucode.etp.server.web.common.exception.SystemException;
import com.xiaoniucode.etp.server.web.dto.binding.*;
import com.xiaoniucode.etp.server.web.entity.CertDomainBinding;
import com.xiaoniucode.etp.server.web.entity.ProxyDO;
import com.xiaoniucode.etp.server.web.entity.ProxyDomainDO;
import com.xiaoniucode.etp.server.web.entity.SslCertDO;
import com.xiaoniucode.etp.server.web.enums.BindStatus;
import com.xiaoniucode.etp.server.web.enums.SslStatus;
import com.xiaoniucode.etp.server.web.param.binding.CertBindParam;
import com.xiaoniucode.etp.server.web.param.binding.CertBindPreviewParam;
import com.xiaoniucode.etp.server.web.param.binding.CertRebindParam;
import com.xiaoniucode.etp.server.web.repository.CertDomainBindingRepository;
import com.xiaoniucode.etp.server.web.repository.ProxyDomainRepository;
import com.xiaoniucode.etp.server.web.repository.ProxyRepository;
import com.xiaoniucode.etp.server.web.repository.SslCertRepository;
import com.xiaoniucode.etp.server.web.service.CertBindingService;
import com.xiaoniucode.etp.server.web.service.DomainCertMatcher;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CertBindingServiceImpl implements CertBindingService {

    private static final Logger logger = LoggerFactory.getLogger(CertBindingServiceImpl.class);

    private final CertDomainBindingRepository bindingRepository;
    private final SslCertRepository sslCertRepository;
    private final ProxyDomainRepository proxyDomainRepository;
    private final ProxyRepository proxyRepository;
    private final SslCertificateManager sslCertificateManager;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CertBindResultDTO bind(CertBindParam param) {
        SslCertDO cert = requireActiveCert(param.getCertId());
        List<String> sanDomains = DomainCertMatcher.parseSanDomains(cert.getSanDomains());
        File keyFile = requireCertFile(cert.getKeyPath(), "证书私钥文件不存在");
        File certFile = requireCertFile(cert.getFullChainPath(), "证书链文件不存在");

        boolean override = param.getOverride() == null || param.getOverride();
        CertBindResultDTO result = new CertBindResultDTO();
        for (Long proxyDomainId : param.getProxyDomainIds()) {
            CertBindItemResultDTO item = bindSingleDomain(
                    proxyDomainId, cert, sanDomains, keyFile, certFile, override);
            result.getResults().add(item);
            if ("ACTIVE".equals(item.getStatus())) {
                result.setSuccessCount(result.getSuccessCount() + 1);
            } else {
                result.setFailedCount(result.getFailedCount() + 1);
            }
        }
        return result;
    }

    @Override
    public List<CertBindPreviewItemDTO> preview(CertBindPreviewParam param) {
        SslCertDO cert = sslCertRepository.findById(param.getCertId())
                .orElseThrow(() -> new BizException("证书不存在"));
        List<String> sanDomains = DomainCertMatcher.parseSanDomains(cert.getSanDomains());
        Map<Long, CertDomainBinding> bindingMap = loadBindingMap(param.getProxyDomainIds());
        return buildPreviewItems(param.getProxyDomainIds(), sanDomains, bindingMap);
    }

    @Override
    public List<CertBindPreviewItemDTO> listBindableDomains(String certId) {
        SslCertDO cert = sslCertRepository.findById(certId)
                .orElseThrow(() -> new BizException("证书不存在"));
        List<String> sanDomains = DomainCertMatcher.parseSanDomains(cert.getSanDomains());

        List<ProxyDO> httpsProxies = proxyRepository.findByProtocol(ProtocolType.HTTPS);
        if (CollectionUtils.isEmpty(httpsProxies)) {
            return Collections.emptyList();
        }
        List<String> proxyIds = httpsProxies.stream().map(ProxyDO::getId).toList();
        List<ProxyDomainDO> domains = proxyDomainRepository.findByProxyIdIn(proxyIds);
        if (CollectionUtils.isEmpty(domains)) {
            return Collections.emptyList();
        }
        List<Long> proxyDomainIds = domains.stream().map(ProxyDomainDO::getId).toList();
        Map<Long, CertDomainBinding> bindingMap = loadBindingMap(proxyDomainIds);
        return buildPreviewItems(proxyDomainIds, sanDomains, bindingMap);
    }

    @Override
    public ProxyCertMatrixDTO getProxyCertMatrix(String proxyId) {
        proxyRepository.findById(proxyId).orElseThrow(() -> new BizException("代理不存在"));
        List<ProxyDomainDO> domains = proxyDomainRepository.findByProxyId(proxyId);

        ProxyCertMatrixDTO matrix = new ProxyCertMatrixDTO();
        matrix.setProxyId(proxyId);
        if (CollectionUtils.isEmpty(domains)) {
            return matrix;
        }

        List<Long> proxyDomainIds = domains.stream().map(ProxyDomainDO::getId).toList();
        Map<Long, CertDomainBinding> bindingMap = loadBindingMap(proxyDomainIds);
        Map<String, SslCertDO> certMap = loadCertMap(bindingMap.values());

        int boundCount = 0;
        int warningCount = 0;
        for (ProxyDomainDO domain : domains) {
            ProxyDomainCertItemDTO item = new ProxyDomainCertItemDTO();
            item.setProxyDomainId(domain.getId());
            item.setFullDomain(domain.getFullDomain());
            item.setDomainType(domain.getDomainType() != null ? domain.getDomainType().getCode() : null);

            CertDomainBinding binding = bindingMap.get(domain.getId());
            if (binding != null) {
                item.setBinding(toBindingDTO(binding, certMap.get(binding.getCertId())));
                boundCount++;
                if (binding.getStatus() != BindStatus.ACTIVE) {
                    warningCount++;
                }
            } else {
                matrix.setUnboundCount(matrix.getUnboundCount() + 1);
            }
            matrix.getDomains().add(item);
        }

        matrix.setTotalDomains(domains.size());
        matrix.setBoundCount(boundCount);
        matrix.setUnboundCount(domains.size() - boundCount);
        matrix.setWarningCount(warningCount);
        return matrix;
    }

    @Override
    public CertUsageDTO getCertUsage(String certId) {
        sslCertRepository.findById(certId).orElseThrow(() -> new BizException("证书不存在"));
        List<CertDomainBinding> bindings = bindingRepository.findByCertId(certId);

        CertUsageDTO usage = new CertUsageDTO();
        usage.setCertId(certId);
        usage.setUsageCount(bindings.size());
        for (CertDomainBinding binding : bindings) {
            CertUsageDomainDTO item = new CertUsageDomainDTO();
            item.setBindingId(binding.getId());
            item.setProxyDomainId(binding.getProxyDomainId());
            item.setFullDomain(binding.getDomain());
            item.setBindStatus(binding.getStatus().getCode());
            item.setEnabled(binding.getEnabled());
            proxyDomainRepository.findById(binding.getProxyDomainId())
                    .ifPresent(domain -> item.setProxyId(domain.getProxyId()));
            usage.getDomains().add(item);
        }
        return usage;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void disable(Long bindingId) {
        CertDomainBinding binding = requireBinding(bindingId);
        binding.setEnabled(false);
        binding.setStatus(BindStatus.DISABLED);
        bindingRepository.save(binding);
        sslCertificateManager.cancelDeploy(binding.getDomain());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void enable(Long bindingId) {
        CertDomainBinding binding = requireBinding(bindingId);
        SslCertDO cert = requireActiveCert(binding.getCertId());
        List<String> sanDomains = DomainCertMatcher.parseSanDomains(cert.getSanDomains());
        if (!DomainCertMatcher.matches(binding.getDomain(), sanDomains)) {
            binding.setStatus(BindStatus.SAN_MISMATCH);
            bindingRepository.save(binding);
            throw new BizException("证书 SAN 不匹配域名: " + binding.getDomain());
        }
        deployBinding(binding, cert);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unbind(Long bindingId) {
        CertDomainBinding binding = requireBinding(bindingId);
        sslCertificateManager.cancelDeploy(binding.getDomain());
        bindingRepository.delete(binding);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rebind(Long bindingId, CertRebindParam param) {
        CertDomainBinding binding = requireBinding(bindingId);
        SslCertDO cert = requireActiveCert(param.getCertId());
        List<String> sanDomains = DomainCertMatcher.parseSanDomains(cert.getSanDomains());
        if (!DomainCertMatcher.matches(binding.getDomain(), sanDomains)) {
            throw new BizException("证书 SAN 不匹配域名: " + binding.getDomain());
        }
        sslCertificateManager.cancelDeploy(binding.getDomain());
        binding.setCertId(cert.getId());
        binding.setEnabled(true);
        deployBinding(binding, cert);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void redeploy(Long bindingId) {
        CertDomainBinding binding = requireBinding(bindingId);
        if (!Boolean.TRUE.equals(binding.getEnabled())) {
            throw new BizException("绑定已禁用，请先启用");
        }
        SslCertDO cert = requireActiveCert(binding.getCertId());
        deployBinding(binding, cert);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void disableAllByProxy(String proxyId) {
        List<ProxyDomainDO> domains = proxyDomainRepository.findByProxyId(proxyId);
        if (CollectionUtils.isEmpty(domains)) {
            return;
        }
        List<Long> proxyDomainIds = domains.stream().map(ProxyDomainDO::getId).toList();
        List<CertDomainBinding> bindings = bindingRepository.findByProxyDomainIdIn(proxyDomainIds);
        for (CertDomainBinding binding : bindings) {
            binding.setEnabled(false);
            binding.setStatus(BindStatus.DISABLED);
            sslCertificateManager.cancelDeploy(binding.getDomain());
        }
        bindingRepository.saveAll(bindings);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CertBindResultDTO bindMatchingDomainsForProxy(String certId, String proxyId, boolean override) {
        SslCertDO cert = requireActiveCert(certId);
        List<String> sanDomains = DomainCertMatcher.parseSanDomains(cert.getSanDomains());
        List<ProxyDomainDO> domains = proxyDomainRepository.findByProxyId(proxyId);
        List<Long> matchedIds = domains.stream()
                .filter(domain -> DomainCertMatcher.matches(domain.getFullDomain(), sanDomains))
                .map(ProxyDomainDO::getId)
                .toList();
        if (matchedIds.isEmpty()) {
            throw new BizException("没有与证书 SAN 匹配的域名");
        }
        CertBindParam param = new CertBindParam();
        param.setCertId(certId);
        param.setProxyDomainIds(matchedIds);
        param.setOverride(override);
        return bind(param);
    }

    private CertBindItemResultDTO bindSingleDomain(Long proxyDomainId,
                                                   SslCertDO cert,
                                                   List<String> sanDomains,
                                                   File keyFile,
                                                   File certFile,
                                                   boolean override) {
        CertBindItemResultDTO item = new CertBindItemResultDTO();
        item.setProxyDomainId(proxyDomainId);

        ProxyDomainDO proxyDomain = proxyDomainRepository.findById(proxyDomainId)
                .orElse(null);
        if (proxyDomain == null) {
            item.setStatus("FAILED");
            item.setReason("代理域名不存在");
            return item;
        }
        item.setFullDomain(proxyDomain.getFullDomain());

        if (!DomainCertMatcher.matches(proxyDomain.getFullDomain(), sanDomains)) {
            item.setStatus("FAILED");
            item.setReason("域名不在证书 SAN 中");
            return item;
        }

        Optional<CertDomainBinding> existingOpt = bindingRepository.findByProxyDomainId(proxyDomainId);
        if (existingOpt.isPresent() && !override) {
            item.setStatus("FAILED");
            item.setReason("域名已绑定其他证书");
            item.setBindingId(existingOpt.get().getId());
            return item;
        }

        CertDomainBinding binding = existingOpt.orElseGet(CertDomainBinding::new);
        if (existingOpt.isPresent()) {
            sslCertificateManager.cancelDeploy(binding.getDomain());
        }
        binding.setProxyDomainId(proxyDomainId);
        binding.setCertId(cert.getId());
        binding.setDomain(proxyDomain.getFullDomain());
        binding.setEnabled(true);

        try {
            sslCertificateManager.deploy(cert.getId(), binding.getDomain(), keyFile, certFile);
            binding.setStatus(BindStatus.ACTIVE);
            binding.setDeployVersion(Optional.ofNullable(binding.getDeployVersion()).orElse(0) + 1);
            binding.setLastDeployedAt(LocalDateTime.now());
            bindingRepository.save(binding);
            item.setBindingId(binding.getId());
            item.setStatus("ACTIVE");
        } catch (Exception e) {
            logger.error("部署证书失败: {}", binding.getDomain(), e);
            binding.setStatus(BindStatus.DEPLOY_FAILED);
            bindingRepository.save(binding);
            item.setBindingId(binding.getId());
            item.setStatus("FAILED");
            item.setReason("部署失败: " + e.getMessage());
        }
        return item;
    }

    private void deployBinding(CertDomainBinding binding, SslCertDO cert) {
        File keyFile = requireCertFile(cert.getKeyPath(), "证书私钥文件不存在");
        File certFile = requireCertFile(cert.getFullChainPath(), "证书链文件不存在");
        try {
            sslCertificateManager.deploy(cert.getId(), binding.getDomain(), keyFile, certFile);
            binding.setStatus(BindStatus.ACTIVE);
            binding.setEnabled(true);
            binding.setDeployVersion(Optional.ofNullable(binding.getDeployVersion()).orElse(0) + 1);
            binding.setLastDeployedAt(LocalDateTime.now());
            bindingRepository.save(binding);
        } catch (Exception e) {
            binding.setStatus(BindStatus.DEPLOY_FAILED);
            bindingRepository.save(binding);
            throw new SystemException("SSL 证书部署失败: " + binding.getDomain(), e);
        }
    }

    private List<CertBindPreviewItemDTO> buildPreviewItems(List<Long> proxyDomainIds,
                                                           List<String> sanDomains,
                                                           Map<Long, CertDomainBinding> bindingMap) {
        if (CollectionUtils.isEmpty(proxyDomainIds)) {
            return Collections.emptyList();
        }
        List<ProxyDomainDO> domains = proxyDomainRepository.findAllById(proxyDomainIds);
        Map<Long, ProxyDomainDO> domainMap = domains.stream()
                .collect(Collectors.toMap(ProxyDomainDO::getId, d -> d, (a, b) -> a));

        Set<String> proxyIds = domains.stream().map(ProxyDomainDO::getProxyId).collect(Collectors.toSet());
        Map<String, String> proxyNameMap = proxyRepository.findAllById(proxyIds).stream()
                .collect(Collectors.toMap(ProxyDO::getId, ProxyDO::getName, (a, b) -> a));

        List<CertBindPreviewItemDTO> items = new ArrayList<>();
        for (Long proxyDomainId : proxyDomainIds) {
            ProxyDomainDO domain = domainMap.get(proxyDomainId);
            if (domain == null) {
                continue;
            }
            CertBindPreviewItemDTO item = new CertBindPreviewItemDTO();
            item.setProxyDomainId(proxyDomainId);
            item.setFullDomain(domain.getFullDomain());
            item.setProxyId(domain.getProxyId());
            item.setProxyName(proxyNameMap.get(domain.getProxyId()));
            boolean matched = DomainCertMatcher.matches(domain.getFullDomain(), sanDomains);
            item.setMatched(matched);
            if (!matched) {
                item.setReason("域名不在证书 SAN 中");
            }
            CertDomainBinding binding = bindingMap.get(proxyDomainId);
            item.setHasBinding(binding != null);
            if (binding != null) {
                item.setCurrentCertId(binding.getCertId());
            }
            items.add(item);
        }
        return items;
    }

    private Map<Long, CertDomainBinding> loadBindingMap(Collection<Long> proxyDomainIds) {
        if (CollectionUtils.isEmpty(proxyDomainIds)) {
            return Collections.emptyMap();
        }
        return bindingRepository.findByProxyDomainIdIn(proxyDomainIds).stream()
                .collect(Collectors.toMap(CertDomainBinding::getProxyDomainId, b -> b, (a, b) -> a));
    }

    private Map<String, SslCertDO> loadCertMap(Collection<CertDomainBinding> bindings) {
        Set<String> certIds = bindings.stream().map(CertDomainBinding::getCertId).collect(Collectors.toSet());
        if (certIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return sslCertRepository.findAllById(certIds).stream()
                .collect(Collectors.toMap(SslCertDO::getId, c -> c, (a, b) -> a));
    }

    private DomainCertBindingDTO toBindingDTO(CertDomainBinding binding, SslCertDO cert) {
        DomainCertBindingDTO dto = new DomainCertBindingDTO();
        dto.setBindingId(binding.getId());
        dto.setCertId(binding.getCertId());
        dto.setBindStatus(binding.getStatus().getCode());
        dto.setEnabled(binding.getEnabled());
        if (cert != null) {
            dto.setIssuer(cert.getIssuer());
            dto.setOrg(cert.getOrg());
            dto.setNotAfter(cert.getNotAfter());
            dto.setCertSanDomains(DomainCertMatcher.parseSanDomains(cert.getSanDomains()));
        }
        return dto;
    }

    private CertDomainBinding requireBinding(Long bindingId) {
        return bindingRepository.findById(bindingId)
                .orElseThrow(() -> new BizException("绑定记录不存在"));
    }

    private SslCertDO requireActiveCert(String certId) {
        SslCertDO cert = sslCertRepository.findById(certId)
                .orElseThrow(() -> new BizException("证书不存在"));
        if (cert.getStatus() == SslStatus.EXPIRED) {
            throw new BizException("证书已过期");
        }
        LocalDate notAfter = cert.getNotAfter();
        if (notAfter != null && LocalDate.now().isAfter(notAfter)) {
            throw new BizException("证书已过期");
        }
        return cert;
    }

    private File requireCertFile(String path, String message) {
        if (!StringUtils.hasText(path)) {
            throw new SystemException(message);
        }
        File file = new File(path);
        if (!file.exists()) {
            throw new SystemException(message);
        }
        return file;
    }
}
