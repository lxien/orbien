/*
 *    Copyright 2026 lxien
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package io.github.lxien.orbien.server.web.service.converter;

import io.github.lxien.orbien.server.web.dto.binding.DomainCertBindingDTO;
import io.github.lxien.orbien.server.web.dto.proxy.TlsCertSummaryDTO;
import io.github.lxien.orbien.server.web.entity.CertDomainBinding;
import io.github.lxien.orbien.server.web.entity.ProxyDomainDO;
import io.github.lxien.orbien.server.web.entity.TlsCertDO;
import io.github.lxien.orbien.server.web.enums.BindStatus;
import io.github.lxien.orbien.server.web.service.DomainCertMatcher;
import org.mapstruct.Mapper;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Mapper(componentModel = "spring")
public interface CertBindingConvert {

    default TlsCertSummaryDTO emptyTlsCertSummary() {
        TlsCertSummaryDTO summary = new TlsCertSummaryDTO();
        summary.setTotalDomains(0);
        summary.setDeployedCount(0);
        summary.setWarningCount(0);
        return summary;
    }

    default boolean isDeployedBinding(CertDomainBinding binding) {
        return binding != null
                && binding.getStatus() == BindStatus.ACTIVE
                && Boolean.TRUE.equals(binding.getEnabled());
    }

    default TlsCertSummaryDTO toTlsCertSummary(List<ProxyDomainDO> domains,
                                               Map<Long, CertDomainBinding> bindingMap) {
        if (domains == null || domains.isEmpty()) {
            return emptyTlsCertSummary();
        }

        TlsCertSummaryDTO summary = new TlsCertSummaryDTO();
        summary.setTotalDomains(domains.size());

        int deployedCount = 0;
        int warningCount = 0;
        for (ProxyDomainDO domain : domains) {
            CertDomainBinding binding = bindingMap.get(domain.getId());
            if (binding == null) {
                continue;
            }
            if (isDeployedBinding(binding)) {
                deployedCount++;
            } else {
                warningCount++;
            }
        }
        summary.setDeployedCount(deployedCount);
        summary.setWarningCount(warningCount);
        return summary;
    }

    default Map<String, TlsCertSummaryDTO> toTlsCertSummaryMap(Collection<String> proxyIds,
                                                               Map<String, List<ProxyDomainDO>> domainsByProxyId,
                                                               Map<Long, CertDomainBinding> bindingMap) {
        Map<String, TlsCertSummaryDTO> summaryMap = new LinkedHashMap<>();
        for (String proxyId : proxyIds) {
            List<ProxyDomainDO> proxyDomains = domainsByProxyId.getOrDefault(proxyId, Collections.emptyList());
            summaryMap.put(proxyId, toTlsCertSummary(proxyDomains, bindingMap));
        }
        return summaryMap;
    }

    default DomainCertBindingDTO toBindingDTO(CertDomainBinding binding, TlsCertDO cert) {
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
}
