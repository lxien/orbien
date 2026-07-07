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
import io.github.lxien.orbien.server.web.entity.ProxyDomainDO;
import io.github.lxien.orbien.server.web.repository.CertDomainBindingRepository;
import io.github.lxien.orbien.server.web.repository.ProxyDomainRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CertBindingSyncService {

    private final CertDomainBindingRepository bindingRepository;
    private final ProxyDomainRepository proxyDomainRepository;
    private final TlsCertificateManager tlsCertificateManager;

    @Transactional(rollbackFor = Exception.class)
    public void removeBindingsByProxyId(String proxyId) {
        removeBindingsByProxyIds(List.of(proxyId));
    }

    @Transactional(rollbackFor = Exception.class)
    public void removeBindingsByProxyIds(List<String> proxyIds) {
        if (CollectionUtils.isEmpty(proxyIds)) {
            return;
        }
        List<ProxyDomainDO> domains = proxyDomainRepository.findByProxyIdIn(proxyIds);
        if (CollectionUtils.isEmpty(domains)) {
            return;
        }
        List<Long> proxyDomainIds = domains.stream().map(ProxyDomainDO::getId).toList();
        removeBindingsByProxyDomainIds(proxyDomainIds);
    }

    @Transactional(rollbackFor = Exception.class)
    public void removeBindingsByProxyDomainIds(List<Long> proxyDomainIds) {
        if (CollectionUtils.isEmpty(proxyDomainIds)) {
            return;
        }
        List<CertDomainBinding> bindings = bindingRepository.findByProxyDomainIdIn(proxyDomainIds);
        for (CertDomainBinding binding : bindings) {
            tlsCertificateManager.cancelDeploy(binding.getDomain());
        }
        bindingRepository.deleteByProxyDomainIdIn(proxyDomainIds);
    }
}
