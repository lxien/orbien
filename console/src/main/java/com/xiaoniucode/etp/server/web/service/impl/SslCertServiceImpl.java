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
import com.xiaoniucode.etp.server.uid.UidGenerator;
import com.xiaoniucode.etp.server.web.common.exception.BizException;
import com.xiaoniucode.etp.server.web.common.exception.SystemException;
import com.xiaoniucode.etp.server.web.common.message.PageQuery;
import com.xiaoniucode.etp.server.web.common.message.PageResult;
import com.xiaoniucode.etp.server.web.common.utils.DateUtil;
import com.xiaoniucode.etp.server.web.common.utils.SslParser;
import com.xiaoniucode.etp.server.web.dto.ssl.SslCertDTO;
import com.xiaoniucode.etp.server.web.dto.ssl.SslCertDownloadDTO;
import com.xiaoniucode.etp.server.web.entity.CertDeployDO;
import com.xiaoniucode.etp.server.web.entity.SslCertDO;
import com.xiaoniucode.etp.server.web.enums.SslStatus;
import com.xiaoniucode.etp.server.web.param.ssl.SslCertDeployParam;
import com.xiaoniucode.etp.server.web.param.ssl.SslCertSaveAndDeployParam;
import com.xiaoniucode.etp.server.web.param.ssl.SslCertSaveParam;
import com.xiaoniucode.etp.server.web.repository.CertDeployRepository;
import com.xiaoniucode.etp.server.web.repository.SslCertRepository;
import com.xiaoniucode.etp.server.web.service.CertDeployService;
import com.xiaoniucode.etp.server.web.service.SslCertificateService;
import com.xiaoniucode.etp.server.web.service.converter.SslCertConvert;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
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
public class SslCertServiceImpl implements SslCertificateService {
    @Autowired
    private SslCertRepository sslCertRepository;
    @Autowired
    private CertDeployRepository certDeployRepository;
    @Autowired
    private SslCertConvert sslCertConvert;
    @Autowired
    private UidGenerator uidGenerator;
    @Autowired
    private CertDeployService certDeployService;
    @PersistenceContext
    private EntityManager entityManager;
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
        return sslCertConvert.toDTO(sslCertDO);
    }

    @Override
    public PageResult<SslCertDTO> findByPage(PageQuery pageQuery) {
        int currentPage = Math.max(0, pageQuery.getCurrent() - 1);
        Pageable pageable = PageRequest.of(currentPage, pageQuery.getSize(), Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<SslCertDO> resultPage = sslCertRepository.findAll(pageable);
        if (resultPage.isEmpty()) {
            return PageResult.empty(pageQuery.getCurrent(), pageQuery.getSize());
        }
        List<SslCertDTO> dtoList = sslCertConvert.toDTOList(resultPage.getContent());
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

        if (certDeployRepository.existsByCertIdIn(ids)) {
            throw new BizException("证书已被部署使用，无法删除");
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
    public void saveAndDeployCert(SslCertSaveAndDeployParam param) {
        String proxyId = param.getProxyId();
        // 解析证书
        SslParser.SslInfo sslInfo = SslParser.parsePem(param.getFullChain());
        if (sslInfo.hasError()) {
            throw new BizException("证书不可用");
        }
        String sha256Fingerprint = sslInfo.getSha256Fingerprint();
        //根据证书指纹查询证书
        SslCertDO sslCertDO = sslCertRepository.findByFingerprint(sha256Fingerprint);
        boolean existsCerts = sslCertDO != null;
        String certId;
        //如果证书不存在，则创建一个证书
        if (!existsCerts) {
            certId = this.saveCert(new SslCertSaveParam(param.getKey(), param.getFullChain())).getId();
        } else {
            certId = sslCertDO.getId();
        }
        CertDeployDO certDeployDO = certDeployRepository.findByProxyId(proxyId);

        if (certDeployDO != null) {
            certDeployService.deleteDeploy(certDeployDO.getId());
            entityManager.flush();
            entityManager.clear();
        }
        certDeployService.deployAndOverride(new SslCertDeployParam(certId, List.of(proxyId)));
    }
}
