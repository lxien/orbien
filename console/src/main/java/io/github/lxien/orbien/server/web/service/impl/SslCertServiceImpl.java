/*
 *
 *  *    Copyright 2026 lxien
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

package io.github.lxien.orbien.server.web.service.impl;

import io.github.lxien.orbien.server.config.SystemConstants;
import io.github.lxien.orbien.server.uid.UidGenerator;
import io.github.lxien.orbien.server.web.common.exception.BizException;
import io.github.lxien.orbien.server.web.common.exception.SystemException;
import io.github.lxien.orbien.server.web.common.message.PageQuery;
import io.github.lxien.orbien.server.web.common.message.PageResult;
import io.github.lxien.orbien.server.web.common.utils.DateUtil;
import io.github.lxien.orbien.server.web.common.utils.SslParser;
import io.github.lxien.orbien.server.web.dto.binding.CertBindResultDTO;
import io.github.lxien.orbien.server.web.dto.ssl.SslCertAutoRenewResultDTO;
import io.github.lxien.orbien.server.web.dto.ssl.SslCertDTO;
import io.github.lxien.orbien.server.web.dto.ssl.SslCertDownloadDTO;
import io.github.lxien.orbien.server.web.entity.SslCertDO;
import io.github.lxien.orbien.server.web.enums.CertSource;
import io.github.lxien.orbien.server.web.enums.ScheduledJobCode;
import io.github.lxien.orbien.server.web.enums.SslStatus;
import io.github.lxien.orbien.server.web.param.binding.CertBindParam;
import io.github.lxien.orbien.server.web.param.ssl.SslCertSaveAndDeployParam;
import io.github.lxien.orbien.server.web.param.ssl.SslCertSaveParam;
import io.github.lxien.orbien.server.web.repository.CertDomainBindingRepository;
import io.github.lxien.orbien.server.web.repository.SslCertRepository;
import io.github.lxien.orbien.server.web.service.CertBindingService;
import io.github.lxien.orbien.server.web.service.SslCertificateService;
import io.github.lxien.orbien.server.web.service.scheduled.ScheduledJobEnableSupport;
import io.github.lxien.orbien.server.web.service.converter.SslCertConvert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import jakarta.servlet.http.HttpServletResponse;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class SslCertServiceImpl implements SslCertificateService {
    @Autowired
    private SslCertRepository sslCertRepository;
    @Autowired
    private CertDomainBindingRepository certDomainBindingRepository;
    @Autowired
    private SslCertConvert sslCertConvert;
    @Autowired
    private UidGenerator uidGenerator;
    @Autowired
    private CertBindingService certBindingService;
    @Autowired
    private ScheduledJobEnableSupport scheduledJobEnableSupport;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SslCertDTO saveCert(SslCertSaveParam param) {
        SslParser.SslInfo sslInfo = SslParser.parsePem(param.getFullChain());
        if (sslInfo.hasError()) {
            throw new BizException("证书不可用");
        }
        String sha256Fingerprint = sslInfo.getSha256Fingerprint();
        if (sslCertRepository.existsByFingerprint(sha256Fingerprint)) {
            throw new BizException("该证书已经存在");
        }
        String certId = uidGenerator.getUIDAsString();

        SslCertDO sslCertDO = new SslCertDO();
        sslCertDO.setId(certId);
        sslCertDO.setIssuer(sslInfo.issuer);
        sslCertDO.setOrg(sslInfo.organization);
        sslCertDO.setSanDomains(String.join(",", sslInfo.dns));
        sslCertDO.setFingerprint(sslInfo.sha256Fingerprint);
        sslCertDO.setSource(CertSource.MANUAL);
        LocalDate notBefore = DateUtil.toLocalDate(sslInfo.issuedAt);
        LocalDate notAfter = DateUtil.toLocalDate(sslInfo.expiresAt);
        sslCertDO.setNotBefore(notBefore);
        sslCertDO.setNotAfter(notAfter);

        LocalDate today = LocalDate.now();
        SslStatus status = (notAfter != null && today.isAfter(notAfter)) ? SslStatus.EXPIRED : SslStatus.ACTIVE;
        sslCertDO.setStatus(status);

        String rootPath = SystemConstants.DEFAULT_DOMAIN_SSL_PATH;

        File rootDir = new File(rootPath);
        if (!rootDir.exists() && !rootDir.mkdirs()) {
            throw new SystemException("无法创建证书目录");
        }

        String certPath = rootPath + File.separator + certId;
        File certDir = new File(certPath);
        if (!certDir.exists() && !certDir.mkdirs()) {
            throw new SystemException("无法创建证书目录: " + certId);
        }

        File keyFile = new File(certPath + File.separator + "privkey.pem");
        File fullChainFile = new File(certPath + File.separator + "fullchain.pem");

        try {
            Files.writeString(keyFile.toPath(), param.getKey(), StandardCharsets.UTF_8);
            Files.writeString(fullChainFile.toPath(), param.getFullChain(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new SystemException("写入证书文件失败");
        }

        sslCertDO.setKeyPath(keyFile.getAbsolutePath());
        sslCertDO.setFullChainPath(fullChainFile.getAbsolutePath());

        sslCertRepository.saveAndFlush(sslCertDO);
        return sslCertConvert.toDTO(sslCertDO, 0L);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SslCertDTO saveAcmeCert(String keyPem, String fullChainPem) {
        SslParser.SslInfo sslInfo = SslParser.parsePem(fullChainPem);
        if (sslInfo.hasError()) {
            throw new BizException("证书不可用");
        }
        String fingerprint = sslInfo.getSha256Fingerprint();
        SslCertDO existing = sslCertRepository.findByFingerprint(fingerprint);
        if (existing != null) {
            return sslCertConvert.toDTO(existing, certDomainBindingRepository.countByCertId(existing.getId()));
        }

        SslCertSaveParam param = new SslCertSaveParam(keyPem, fullChainPem);
        SslCertDTO created = saveCert(param);
        SslCertDO sslCertDO = sslCertRepository.findById(created.getId()).orElseThrow();
        sslCertDO.setSource(CertSource.ACME);
        sslCertRepository.save(sslCertDO);
        created.setSource(CertSource.ACME.getCode());
        return created;
    }

    @Override
    public PageResult<SslCertDTO> findByPage(PageQuery pageQuery) {
        int currentPage = Math.max(0, pageQuery.getCurrent() - 1);
        Pageable pageable = PageRequest.of(currentPage, pageQuery.getSize(), Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<SslCertDO> resultPage = sslCertRepository.findAll(pageable);
        if (resultPage.isEmpty()) {
            return PageResult.empty(pageQuery.getCurrent(), pageQuery.getSize());
        }
        List<String> certIds = resultPage.getContent().stream().map(SslCertDO::getId).toList();
        Map<String, Long> usageCountMap = certDomainBindingRepository.findByCertIdIn(certIds).stream()
                .collect(Collectors.groupingBy(
                        binding -> binding.getCertId(),
                        Collectors.counting()
                ));
        List<SslCertDTO> dtoList = resultPage.getContent().stream()
                .map(cert -> sslCertConvert.toDTO(cert, usageCountMap.getOrDefault(cert.getId(), 0L)))
                .toList();
        return PageResult.wrap(resultPage, dtoList);
    }

    @Override
    public SslCertDownloadDTO getSslDownloadInfo(String certId) {
        Optional<SslCertDO> opt = sslCertRepository.findById(certId);
        if (opt.isEmpty()) {
            return null;
        }
        SslCertDO sslCertDO = opt.get();
        String keyPath = sslCertDO.getKeyPath();
        String fullChainPath = sslCertDO.getFullChainPath();

        if (keyPath == null || fullChainPath == null) {
            throw new SystemException("证书文件路径未配置");
        }

        try {
            String keyPem = Files.readString(new File(keyPath).toPath(), StandardCharsets.UTF_8);
            String fullChainPem = Files.readString(new File(fullChainPath).toPath(), StandardCharsets.UTF_8);

            SslCertDownloadDTO dto = new SslCertDownloadDTO();
            dto.setKeyPem(keyPem);
            dto.setFullChainPem(fullChainPem);
            return dto;
        } catch (IOException e) {
            throw new SystemException("读取证书文件失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteByIds(List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }

        if (certDomainBindingRepository.existsByCertIdIn(ids)) {
            throw new BizException("证书已被域名绑定使用，无法删除");
        }

        List<SslCertDO> certificates = sslCertRepository.findAllById(ids);

        for (SslCertDO sslCertDO : certificates) {
            String keyPath = sslCertDO.getKeyPath();
            if (keyPath != null && !keyPath.isEmpty()) {
                try {
                    File keyFile = new File(keyPath);
                    if (keyFile.exists()) {
                        Files.delete(keyFile.toPath());
                    }

                    File certDir = keyFile.getParentFile();
                    if (certDir != null && certDir.exists() && certDir.isDirectory()) {
                        String fullChainPath = certDir.getPath() + File.separator + "fullchain.pem";
                        File fullChainFile = new File(fullChainPath);
                        if (fullChainFile.exists()) {
                            Files.delete(fullChainFile.toPath());
                        }
                        certDir.delete();
                    }
                } catch (IOException e) {
                    throw new SystemException("删除证书文件失败");
                }
            }
        }

        sslCertRepository.deleteAllById(ids);
    }

    @Override
    public void downloadCert(String certId, HttpServletResponse response) {
        SslCertDownloadDTO ssl = getSslDownloadInfo(certId);
        if (ssl == null) {
            throw new BizException("证书信息不存在");
        }

        response.setContentType("application/octet-stream");
        response.setHeader("Content-Disposition", "attachment; filename=cert.zip");

        try (ZipOutputStream zos = new ZipOutputStream(response.getOutputStream())) {
            zos.putNextEntry(new ZipEntry("fullchain.pem"));
            zos.write(ssl.getFullChainPem().getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();

            zos.putNextEntry(new ZipEntry("privkey.pem"));
            zos.write(ssl.getKeyPem().getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
        } catch (IOException e) {
            throw new BizException("证书打包失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CertBindResultDTO saveAndDeployCert(SslCertSaveAndDeployParam param) {
        SslParser.SslInfo sslInfo = SslParser.parsePem(param.getFullChain());
        if (sslInfo.hasError()) {
            throw new BizException("证书不可用");
        }
        String sha256Fingerprint = sslInfo.getSha256Fingerprint();
        SslCertDO sslCertDO = sslCertRepository.findByFingerprint(sha256Fingerprint);
        String certId;
        if (sslCertDO == null) {
            certId = this.saveCert(new SslCertSaveParam(param.getKey(), param.getFullChain())).getId();
        } else {
            certId = sslCertDO.getId();
        }

        if (!CollectionUtils.isEmpty(param.getProxyDomainIds())) {
            CertBindParam bindParam = new CertBindParam();
            bindParam.setCertId(certId);
            bindParam.setProxyDomainIds(param.getProxyDomainIds());
            bindParam.setOverride(true);
            return certBindingService.bind(bindParam);
        }
        return certBindingService.bindMatchingDomainsForProxy(certId, param.getProxyId(), true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SslCertAutoRenewResultDTO updateAutoRenew(String certId, boolean autoRenew) {
        SslCertDO sslCert = sslCertRepository.findById(certId)
                .orElseThrow(() -> new BizException("证书不存在"));
        if (autoRenew) {
            if (sslCert.getSource() != CertSource.ACME) {
                throw new BizException("仅 ACME 证书支持自动续签");
            }
            if (sslCert.getStatus() != SslStatus.ACTIVE) {
                throw new BizException("证书不可用，无法开启自动续签");
            }
        }
        sslCert.setAutoRenew(autoRenew);
        sslCertRepository.save(sslCert);

        boolean jobAutoEnabled = false;
        if (autoRenew) {
            jobAutoEnabled = scheduledJobEnableSupport.enableIfDisabled(ScheduledJobCode.ACME_RENEW.getCode());
        }

        SslCertAutoRenewResultDTO result = new SslCertAutoRenewResultDTO();
        result.setAutoRenew(autoRenew);
        result.setAcmeRenewJobAutoEnabled(jobAutoEnabled);
        return result;
    }
}
