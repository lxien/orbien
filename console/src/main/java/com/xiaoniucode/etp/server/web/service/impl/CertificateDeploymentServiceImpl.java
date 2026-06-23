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

import com.xiaoniucode.etp.server.transport.https.SslCertificateManager;
import com.xiaoniucode.etp.server.web.common.exception.SystemException;
import com.xiaoniucode.etp.server.web.dto.deploy.SslDeployDTO;
import com.xiaoniucode.etp.server.web.dto.deploy.SslDeployInfoDTO;
import com.xiaoniucode.etp.server.web.entity.CertificateDeploymentDO;
import com.xiaoniucode.etp.server.web.entity.SslCertificateDO;
import com.xiaoniucode.etp.server.web.param.ssl.SslCertDeployParam;
import com.xiaoniucode.etp.server.web.repository.CertificateDeploymentRepository;
import com.xiaoniucode.etp.server.web.repository.SslCertificateRepository;
import com.xiaoniucode.etp.server.web.service.CertificateDeploymentService;
import com.xiaoniucode.etp.server.web.service.converter.CertificateDeploymentConvert;
import com.xiaoniucode.etp.server.web.support.tx.TransactionHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

@Service
@RequiredArgsConstructor
public class CertificateDeploymentServiceImpl implements CertificateDeploymentService {
    private final CertificateDeploymentRepository certificateDeploymentRepository;
    @Autowired
    private SslCertificateManager sslCertificateManager;
    @Autowired
    private TransactionHelper transactionHelper;
    @Autowired
    private SslCertificateRepository sslCertificateRepository;
    @Autowired
    private CertificateDeploymentConvert certificateDeploymentConvert;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void closeSsl(String proxyId) {
        CertificateDeploymentDO deploymentDO = certificateDeploymentRepository.findByProxyId(proxyId);
        if (deploymentDO != null) {
            deploymentDO.setEnabled(false);
            certificateDeploymentRepository.saveAndFlush(deploymentDO);
        }
        //todo 需要清理证书管理器
    }

    @Override
    public void deleteDeploy(Long deployId) {

    }

    @Override
    public SslDeployInfoDTO getSslDeployInfo(String proxyId) {
        CertificateDeploymentDO deploymentDO = certificateDeploymentRepository.findByProxyId(proxyId);
        if (deploymentDO == null) {
            return null;
        }

        Long certId = deploymentDO.getCertId();
        SslCertificateDO certificateDO = sslCertificateRepository.findById(certId).orElse(null);
        if (certificateDO == null) {
            return null;
        }

        SslDeployInfoDTO dto = certificateDeploymentConvert.toDeployInfoDTO(deploymentDO, certificateDO);

        String keyPath = certificateDO.getKeyPath();
        String fullChainPath = certificateDO.getFullChainPath();

        if (keyPath != null && fullChainPath != null) {
            try {
                dto.setKeyPem(Files.readString(new File(keyPath).toPath(), StandardCharsets.UTF_8));
                dto.setFullChainPem(Files.readString(new File(fullChainPath).toPath(), StandardCharsets.UTF_8));
            } catch (IOException e) {
                throw new SystemException("读取证书文件失败");
            }
        }

        return dto;
    }

    @Override
    public SslDeployDTO deploy(SslCertDeployParam param) {
        return null;
    }
}
