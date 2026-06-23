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

import com.xiaoniucode.etp.server.config.SystemConstants;
import com.xiaoniucode.etp.server.web.common.exception.BizException;
import com.xiaoniucode.etp.server.web.common.exception.SystemException;
import com.xiaoniucode.etp.server.web.common.message.PageQuery;
import com.xiaoniucode.etp.server.web.common.message.PageResult;
import com.xiaoniucode.etp.server.web.common.utils.DateUtil;
import com.xiaoniucode.etp.server.web.common.utils.SslParser;
import com.xiaoniucode.etp.server.web.dto.ssl.SslCertDTO;
import com.xiaoniucode.etp.server.web.dto.ssl.SslCertDownloadDTO;
import com.xiaoniucode.etp.server.web.entity.SslCertificateDO;
import com.xiaoniucode.etp.server.web.enums.SslStatus;
import com.xiaoniucode.etp.server.web.param.ssl.SslCertSaveParam;
import com.xiaoniucode.etp.server.web.repository.CertificateDeploymentRepository;
import com.xiaoniucode.etp.server.web.repository.SslCertificateRepository;
import com.xiaoniucode.etp.server.web.service.SslCertificateService;
import com.xiaoniucode.etp.server.web.service.converter.SslCertificateConvert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.servlet.http.HttpServletResponse;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class SslCertificateServiceImpl implements SslCertificateService {
    @Autowired
    private SslCertificateRepository sslCertificateRepository;
    @Autowired
    private CertificateDeploymentRepository certificateDeploymentRepository;
    @Autowired
    private SslCertificateConvert sslCertificateConvert;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SslCertDTO saveCert(SslCertSaveParam param) {
        SslParser.SslInfo sslInfo = SslParser.parsePem(param.getFullChain());
        if (sslInfo.hasError()) {
            throw new BizException("证书不可用");
        }
        String sha256Fingerprint = sslInfo.getSha256Fingerprint();
        if (sslCertificateRepository.existsByFingerprint(sha256Fingerprint)) {
            throw new BizException("该证书已经存在");
        }
        
        SslCertificateDO sslCertificateDO = new SslCertificateDO();
        sslCertificateDO.setIssuer(sslInfo.issuer);
        sslCertificateDO.setOrg(sslInfo.organization);
        sslCertificateDO.setSanDomains(String.join(",", sslInfo.dns));
        sslCertificateDO.setFingerprint(sslInfo.sha256Fingerprint);
        LocalDate notBefore = DateUtil.toLocalDate(sslInfo.issuedAt);
        LocalDate notAfter = DateUtil.toLocalDate(sslInfo.expiresAt);
        sslCertificateDO.setNotBefore(notBefore);
        sslCertificateDO.setNotAfter(notAfter);

        LocalDate today = LocalDate.now();
        SslStatus status = (notAfter != null && today.isAfter(notAfter)) ? SslStatus.EXPIRED : SslStatus.ACTIVE;
        sslCertificateDO.setStatus(status);

        String rootPath = SystemConstants.DEFAULT_DOMAIN_SSL_PATH;
        
        File rootDir = new File(rootPath);
        if (!rootDir.exists() && !rootDir.mkdirs()) {
            throw new SystemException("无法创建证书目录");
        }

        String firstKeyPath = null;
        String firstFullChainPath = null;
        
        for (String domain : sslInfo.dns) {
            String domainPath = rootPath + File.separator + domain;
            File domainDir = new File(domainPath);
            if (!domainDir.exists() && !domainDir.mkdirs()) {
                throw new SystemException("无法创建域名目录: " + domain);
            }

            String keyPath = domainPath + File.separator + "privkey.pem";
            String fullChainPath = domainPath + File.separator + "fullchain.pem";

            try {
                Files.writeString(new File(keyPath).toPath(), param.getKey(), StandardCharsets.UTF_8);
                Files.writeString(new File(fullChainPath).toPath(), param.getFullChain(), StandardCharsets.UTF_8);
                
                if (firstKeyPath == null) {
                    firstKeyPath = keyPath;
                    firstFullChainPath = fullChainPath;
                }
            } catch (IOException e) {
                throw new SystemException("写入证书文件失败: " + domain);
            }
        }

        sslCertificateDO.setKeyPath(firstKeyPath);
        sslCertificateDO.setFullChainPath(firstFullChainPath);

        sslCertificateRepository.saveAndFlush(sslCertificateDO);
        return sslCertificateConvert.toDTO(sslCertificateDO);
    }

    @Override
    public PageResult<SslCertDTO> findByPage(PageQuery pageQuery) {
        int currentPage = Math.max(0, pageQuery.getCurrent() - 1);
        Pageable pageable = PageRequest.of(currentPage, pageQuery.getSize(), Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<SslCertificateDO> resultPage = sslCertificateRepository.findAll(pageable);
        if (resultPage.isEmpty()) {
            return PageResult.empty(pageQuery.getCurrent(), pageQuery.getSize());
        }
        List<SslCertDTO> dtoList = sslCertificateConvert.toDTOList(resultPage.getContent());
        return PageResult.wrap(resultPage, dtoList);
    }

    @Override
    public SslCertDownloadDTO getSslDownloadInfo(Long certId) {
        Optional<SslCertificateDO> opt = sslCertificateRepository.findById(certId);
        if (opt.isEmpty()) {
            return null;
        }
        SslCertificateDO sslCertificateDO = opt.get();
        String keyPath = sslCertificateDO.getKeyPath();
        String fullChainPath = sslCertificateDO.getFullChainPath();

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
    public void deleteByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }

        if (certificateDeploymentRepository.existsByCertIdIn(ids)) {
            throw new BizException("证书已被部署使用，无法删除");
        }

        List<SslCertificateDO> certificates = sslCertificateRepository.findAllById(ids);

        for (SslCertificateDO sslCertificateDO : certificates) {
            String sanDomains = sslCertificateDO.getSanDomains();
            if (sanDomains != null && !sanDomains.isEmpty()) {
                String[] domains = sanDomains.split(",");
                String rootPath = SystemConstants.DEFAULT_DOMAIN_SSL_PATH;

                for (String domain : domains) {
                    String domainPath = rootPath + File.separator + domain.trim();
                    File domainDir = new File(domainPath);
                    if (domainDir.exists() && domainDir.isDirectory()) {
                        try {
                            File keyFile = new File(domainPath + File.separator + "privkey.pem");
                            if (keyFile.exists()) {
                                Files.delete(keyFile.toPath());
                            }

                            File fullChainFile = new File(domainPath + File.separator + "fullchain.pem");
                            if (fullChainFile.exists()) {
                                Files.delete(fullChainFile.toPath());
                            }

                            domainDir.delete();
                        } catch (IOException e) {
                            throw new SystemException("删除证书文件失败: " + domain);
                        }
                    }
                }
            }
        }

        sslCertificateRepository.deleteAllById(ids);
    }

    @Override
    public void downloadCert(Long certId, HttpServletResponse response) {
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

}
