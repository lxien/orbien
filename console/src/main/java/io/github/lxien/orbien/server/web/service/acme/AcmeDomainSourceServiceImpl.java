package io.github.lxien.orbien.server.web.service.acme;

import io.github.lxien.orbien.core.enums.DomainType;
import io.github.lxien.orbien.core.enums.ProtocolType;
import io.github.lxien.orbien.server.web.common.exception.BizException;
import io.github.lxien.orbien.server.web.dto.acme.AcmeHttpsProxyDomainOptionDTO;
import io.github.lxien.orbien.server.web.dto.acme.AcmeHttpsProxyOptionDTO;
import io.github.lxien.orbien.server.web.entity.AgentDO;
import io.github.lxien.orbien.server.web.entity.CertDomainBinding;
import io.github.lxien.orbien.server.web.entity.ProxyDO;
import io.github.lxien.orbien.server.web.entity.ProxyDomainDO;
import io.github.lxien.orbien.server.web.entity.TlsCertDO;
import io.github.lxien.orbien.server.web.repository.AgentRepository;
import io.github.lxien.orbien.server.web.repository.CertDomainBindingRepository;
import io.github.lxien.orbien.server.web.repository.ProxyDomainRepository;
import io.github.lxien.orbien.server.web.repository.ProxyRepository;
import io.github.lxien.orbien.server.web.repository.TlsCertRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AcmeDomainSourceServiceImpl implements AcmeDomainSourceService {

    private static final int DOMAIN_PREVIEW_LIMIT = 3;

    private final ProxyRepository proxyRepository;
    private final ProxyDomainRepository proxyDomainRepository;
    private final AgentRepository agentRepository;
    private final CertDomainBindingRepository certDomainBindingRepository;
    private final TlsCertRepository tlsCertRepository;

    @Override
    public List<AcmeHttpsProxyOptionDTO> listHttpsProxyOptions() {
        List<ProxyDO> proxies = proxyRepository.findByProtocolIn(
                List.of(ProtocolType.HTTPS, ProtocolType.FILE));
        if (CollectionUtils.isEmpty(proxies)) {
            return List.of();
        }

        List<String> proxyIds = proxies.stream().map(ProxyDO::getId).toList();
        Map<String, List<ProxyDomainDO>> domainsByProxyId = proxyDomainRepository.findByProxyIdIn(proxyIds).stream()
                .collect(Collectors.groupingBy(ProxyDomainDO::getProxyId));

        List<String> agentIds = proxies.stream().map(ProxyDO::getAgentId).filter(StringUtils::hasText).distinct().toList();
        Map<String, String> agentNameMap = agentRepository.findAllById(agentIds).stream()
                .collect(Collectors.toMap(AgentDO::getId, AgentDO::getName, (a, b) -> a));

        return proxies.stream()
                .sorted(Comparator.comparing(ProxyDO::getUpdatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(proxy -> toProxyOption(proxy, domainsByProxyId.getOrDefault(proxy.getId(), List.of()), agentNameMap))
                .toList();
    }

    @Override
    public List<AcmeHttpsProxyDomainOptionDTO> listDomainsByProxyId(String proxyId) {
        ProxyDO proxy = proxyRepository.findById(proxyId)
                .filter(item -> item.getProtocol().isHttps() || item.getProtocol().isFile())
                .orElseThrow(() -> new BizException("代理不存在或不支持 TLS 证书"));

        List<ProxyDomainDO> domains = proxyDomainRepository.findByProxyId(proxy.getId());
        if (CollectionUtils.isEmpty(domains)) {
            return List.of();
        }

        List<Long> proxyDomainIds = domains.stream().map(ProxyDomainDO::getId).toList();
        Map<Long, CertDomainBinding> bindingMap = certDomainBindingRepository.findByProxyDomainIdIn(proxyDomainIds).stream()
                .collect(Collectors.toMap(CertDomainBinding::getProxyDomainId, item -> item, (a, b) -> a));

        List<String> certIds = bindingMap.values().stream()
                .map(CertDomainBinding::getCertId)
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
        Map<String, TlsCertDO> certMap = certIds.isEmpty()
                ? Collections.emptyMap()
                : tlsCertRepository.findAllById(certIds).stream()
                .collect(Collectors.toMap(TlsCertDO::getId, item -> item, (a, b) -> a));

        return domains.stream()
                .sorted(Comparator.comparing(ProxyDomainDO::getFullDomain, String.CASE_INSENSITIVE_ORDER))
                .map(domain -> toDomainOption(domain, bindingMap.get(domain.getId()), certMap))
                .toList();
    }

    private AcmeHttpsProxyOptionDTO toProxyOption(ProxyDO proxy,
                                                  List<ProxyDomainDO> domains,
                                                  Map<String, String> agentNameMap) {
        AcmeHttpsProxyOptionDTO dto = new AcmeHttpsProxyOptionDTO();
        dto.setProxyId(proxy.getId());
        dto.setName(proxy.getName());
        dto.setAgentId(proxy.getAgentId());
        dto.setAgentName(agentNameMap.getOrDefault(proxy.getAgentId(), proxy.getAgentId()));
        dto.setStatus(proxy.getStatus() != null ? proxy.getStatus().getCode() : null);
        dto.setDomainCount(domains.size());
        dto.setDomainPreview(domains.stream()
                .map(ProxyDomainDO::getFullDomain)
                .filter(StringUtils::hasText)
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .limit(DOMAIN_PREVIEW_LIMIT)
                .toList());
        return dto;
    }

    private AcmeHttpsProxyDomainOptionDTO toDomainOption(ProxyDomainDO domain,
                                                         CertDomainBinding binding,
                                                         Map<String, TlsCertDO> certMap) {
        AcmeHttpsProxyDomainOptionDTO dto = new AcmeHttpsProxyDomainOptionDTO();
        dto.setProxyDomainId(domain.getId());
        dto.setFullDomain(domain.getFullDomain());
        DomainType domainType = domain.getDomainType();
        if (domainType != null) {
            dto.setDomainType(domainType.getCode());
            dto.setDomainTypeLabel(domainType.getDescription());
        }
        dto.setSelectable(StringUtils.hasText(domain.getFullDomain()));
        if (binding != null) {
            dto.setBound(true);
            TlsCertDO cert = certMap.get(binding.getCertId());
            if (cert != null && StringUtils.hasText(cert.getIssuer())) {
                dto.setBoundCertIssuer(cert.getIssuer());
            }
        }
        return dto;
    }
}
