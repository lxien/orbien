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

package io.github.lxien.orbien.server.web.service;

import io.github.lxien.orbien.server.transport.https.TlsCertificateManager;
import io.github.lxien.orbien.server.web.entity.CertDomainBinding;
import io.github.lxien.orbien.server.web.entity.TlsCertDO;
import io.github.lxien.orbien.server.web.enums.BindStatus;
import io.github.lxien.orbien.server.web.repository.CertDomainBindingRepository;
import io.github.lxien.orbien.server.web.repository.TlsCertRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class CertRuntimeLoader {

    private static final Logger logger = LoggerFactory.getLogger(CertRuntimeLoader.class);

    private final CertDomainBindingRepository bindingRepository;
    private final TlsCertRepository tlsCertRepository;
    private final TlsCertificateManager tlsCertificateManager;

    @EventListener(ApplicationReadyEvent.class)
    public void loadAllBindings() {
        List<CertDomainBinding> bindings = bindingRepository.findByStatusAndEnabled(BindStatus.ACTIVE, true);
        if (bindings.isEmpty()) {
            logger.info("未发现需要加载的域名证书绑定");
            return;
        }
        Map<String, TlsCertDO> certMap = tlsCertRepository.findAllById(
                bindings.stream().map(CertDomainBinding::getCertId).collect(Collectors.toSet())
        ).stream().collect(Collectors.toMap(TlsCertDO::getId, c -> c, (a, b) -> a));

        int success = 0;
        int failed = 0;
        for (CertDomainBinding binding : bindings) {
            TlsCertDO cert = certMap.get(binding.getCertId());
            if (cert == null) {
                markDeployFailed(binding);
                failed++;
                continue;
            }
            if (deploy(binding, cert)) {
                success++;
            } else {
                failed++;
            }
        }
        logger.info("域名证书加载完成: 成功 {}, 失败 {}", success, failed);
    }

    private boolean deploy(CertDomainBinding binding, TlsCertDO cert) {
        try {
            File keyFile = new File(cert.getKeyPath());
            File certFile = new File(cert.getFullChainPath());
            if (!keyFile.exists() || !certFile.exists()) {
                markDeployFailed(binding);
                return false;
            }
            tlsCertificateManager.deploy(cert.getId(), binding.getDomain(), keyFile, certFile);
            return true;
        } catch (Exception e) {
            logger.error("启动加载证书失败: {}", binding.getDomain(), e);
            markDeployFailed(binding);
            return false;
        }
    }

    private void markDeployFailed(CertDomainBinding binding) {
        binding.setStatus(BindStatus.DEPLOY_FAILED);
        bindingRepository.save(binding);
    }
}
