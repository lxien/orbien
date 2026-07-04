package io.github.lxien.orbien.server.web.repository;

import io.github.lxien.orbien.server.web.entity.ProxyDomainDO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * 代理域名 Repository
 */
@Repository
public interface ProxyDomainRepository extends JpaRepository<ProxyDomainDO, Long> {

    void deleteByProxyId(String proxyId);

    List<ProxyDomainDO> findByProxyIdIn(List<String> proxyIds);

    List<ProxyDomainDO> findByProxyId(String id);

    void deleteByProxyIdIn(List<String> ids);

    boolean existsByFullDomain(String fullDomain);

    List<ProxyDomainDO> findByFullDomainIn(Collection<String> fullDomains);

    Optional<ProxyDomainDO> findByFullDomain(String fullDomain);
}
