package io.github.lxien.orbien.server.service;

import io.github.lxien.orbien.core.domain.DomainInfo;
import io.github.lxien.orbien.core.domain.ProxyConfigExt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

@Service
public class ProxyCacheEvictionService {

    @Autowired
    private ProxyConfigService proxyConfigService;
    @Autowired
    private DomainConfigService domainConfigService;

    public void evict(ProxyConfigExt ext) {
        if (ext == null || ext.getProxyConfig() == null) {
            return;
        }
        if (!CollectionUtils.isEmpty(ext.getDomains())) {
            for (DomainInfo domain : ext.getDomains()) {
                domainConfigService.evictDomain(domain.getFullDomain());
            }
        }
        proxyConfigService.evictByProxyId(ext.getProxyConfig().getProxyId());
    }

    public void evictByProxyId(String proxyId) {
        if (!StringUtils.hasText(proxyId)) {
            return;
        }
        evict(proxyConfigService.findById(proxyId));
    }
}
