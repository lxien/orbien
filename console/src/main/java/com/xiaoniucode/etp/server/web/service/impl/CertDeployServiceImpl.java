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
import com.xiaoniucode.etp.server.web.common.exception.BizException;
import com.xiaoniucode.etp.server.web.common.exception.SystemException;
import com.xiaoniucode.etp.server.web.dto.deploy.SslDeployDTO;
import com.xiaoniucode.etp.server.web.dto.deploy.SslDeployInfoDTO;
import com.xiaoniucode.etp.server.web.entity.*;
import com.xiaoniucode.etp.server.web.param.ssl.SslCertDeployParam;
import com.xiaoniucode.etp.server.web.repository.*;
import com.xiaoniucode.etp.server.web.service.CertDeployService;
import com.xiaoniucode.etp.server.web.service.converter.CertDeployConvert;
import com.xiaoniucode.etp.server.web.support.tx.TransactionHelper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CertDeployServiceImpl implements CertDeployService {
    private final Logger logger = LoggerFactory.getLogger(CertDeployServiceImpl.class);
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
    @Autowired
    private ProxyRepository proxyRepository;
    @Autowired
    private ProxyDomainRepository proxyDomainRepository;
    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void closeSsl(String proxyId) {
        CertDeployDO deploymentDO = certDeployRepository.findByProxyId(proxyId);
        if (deploymentDO != null) {
            deploymentDO.setEnabled(false);
            certDeployRepository.save(deploymentDO);

            //清理证书管理器部署信息
            certDeployDomainRepository.findByDeployId(deploymentDO.getId()).forEach(domain ->
                    sslCertificateManager.cancelDeploy(domain.getDomain()));
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteDeploy(Long deployId) {
        certDeployDomainRepository.findByDeployId(deployId).forEach(domain ->
                sslCertificateManager.cancelDeploy(domain.getDomain()));
        certDeployRepository.deleteById(deployId);
        certDeployDomainRepository.deleteByDeployId(deployId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchDeleteDeploy(List<Long> deployIds) {
        certDeployDomainRepository.findByDeployIdIn(deployIds).forEach(domain ->
                sslCertificateManager.cancelDeploy(domain.getDomain()));

        certDeployDomainRepository.deleteByDeployIdIn(deployIds);
        certDeployRepository.deleteAllById(deployIds);
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
    @Transactional(rollbackFor = Exception.class)
    public SslDeployDTO deployAndOverride(SslCertDeployParam param) {
        String certId = param.getCertId();
        List<String> proxyIds = param.getProxyIds();

        //根据certId查询证书信息
        SslCertDO sslCertDO = sslCertRepository.findById(certId).orElseThrow(() -> new BizException("证书不存在"));
        String keyPath = sslCertDO.getKeyPath();
        String fullChainPath = sslCertDO.getFullChainPath();
        //加载证书文件为File
        File keyFile = new File(keyPath);
        if (!keyFile.exists()) {
            throw new SystemException("证书私钥文件不存在");
        }
        File certFile = new File(fullChainPath);
        if (!certFile.exists()) {
            throw new SystemException("证书链文件不存在");
        }

        //一次性查询出所有代理,并校验存在性
        List<String> existingProxyIds = proxyRepository.findAllById(proxyIds).stream()
                .map(ProxyDO::getId)
                .toList();
        if (existingProxyIds.size() != proxyIds.size()) {
            proxyIds.removeAll(existingProxyIds);
            throw new BizException("以下代理不存在: " + proxyIds);
        }

        //一次性查询出每个代理的所有域名
        List<ProxyDomainDO> allDomains = proxyDomainRepository.findByProxyIdIn(proxyIds);
        Map<String, List<ProxyDomainDO>> domainMap = allDomains.stream()
                .collect(Collectors.groupingBy(ProxyDomainDO::getProxyId));

        List<CertDeployDO> existingDeployments = certDeployRepository.findByProxyIdIn(proxyIds);
        if (!CollectionUtils.isEmpty(existingDeployments)) {
            List<Long> deployIds = existingDeployments.stream().map(CertDeployDO::getId).toList();
            this.batchDeleteDeploy(deployIds);
            entityManager.flush();
            entityManager.clear();
        }

        List<String> successDomains = new ArrayList<>();
        //将每个代理下的所有域名都进行域名部署
        for (String proxyId : proxyIds) {
            List<ProxyDomainDO> domains = domainMap.getOrDefault(proxyId, Collections.emptyList());

            CertDeployDO certDeployDO = certDeployRepository.save(new CertDeployDO(certId, proxyId, true));
            for (ProxyDomainDO proxyDomainDO : domains) {
                String domain = proxyDomainDO.getFullDomain();
                certDeployDomainRepository.save(new CertDeployDomainDO(certDeployDO.getId(), domain));
                try {
                    sslCertificateManager.deploy(certId, domain, keyFile, certFile);
                    successDomains.add(domain);
                } catch (Exception e) {
                    throw new SystemException("SSL 证书部署失败: " + domain, e);
                }
            }
        }

        SslDeployDTO result = new SslDeployDTO();
        result.setDomains(successDomains);
        return result;
    }
}
