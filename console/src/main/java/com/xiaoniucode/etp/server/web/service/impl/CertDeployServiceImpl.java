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
import com.xiaoniucode.etp.server.web.entity.CertDeployDO;
import com.xiaoniucode.etp.server.web.entity.SslCertDO;
import com.xiaoniucode.etp.server.web.param.ssl.SslCertDeployParam;
import com.xiaoniucode.etp.server.web.repository.CertDeployDomainRepository;
import com.xiaoniucode.etp.server.web.repository.CertDeployRepository;
import com.xiaoniucode.etp.server.web.repository.SslCertRepository;
import com.xiaoniucode.etp.server.web.service.CertDeployService;
import com.xiaoniucode.etp.server.web.service.converter.CertDeployConvert;
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
public class CertDeployServiceImpl implements CertDeployService {
    private final CertDeployRepository certDeployRepository;
    @Autowired
    private SslCertificateManager sslCertificateManager;
    @Autowired
    private TransactionHelper transactionHelper;
    @Autowired
    private SslCertRepository sslCertRepository;
    @Autowired
    private CertDeployConvert certDeployConvert;
    @Autowired
    private CertDeployDomainRepository certDeployDomainRepository;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void closeSsl(String proxyId) {
        CertDeployDO deploymentDO = certDeployRepository.findByProxyId(proxyId);
        if (deploymentDO != null) {
            deploymentDO.setEnabled(false);
            certDeployRepository.saveAndFlush(deploymentDO);
        }

        //清理证书管理器

    }

    @Override
    public void deleteDeploy(Long deployId) {

    }

    @Override
    public SslDeployInfoDTO getSslDeployInfo(String proxyId) {
        CertDeployDO deploymentDO = certDeployRepository.findByProxyId(proxyId);
        if (deploymentDO == null) {
            return null;
        }

        String certId = deploymentDO.getCertId();
        SslCertDO certificateDO = sslCertRepository.findById(certId).orElse(null);
        if (certificateDO == null) {
            return null;
        }

        SslDeployInfoDTO dto = certDeployConvert.toDeployInfoDTO(deploymentDO, certificateDO);

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
