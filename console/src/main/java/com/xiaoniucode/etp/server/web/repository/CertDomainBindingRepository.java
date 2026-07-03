package com.xiaoniucode.etp.server.web.repository;

import com.xiaoniucode.etp.server.web.entity.CertDomainBinding;
import com.xiaoniucode.etp.server.web.enums.BindStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface CertDomainBindingRepository extends JpaRepository<CertDomainBinding, Long> {

    Optional<CertDomainBinding> findByProxyDomainId(Long proxyDomainId);

    List<CertDomainBinding> findByProxyDomainIdIn(Collection<Long> proxyDomainIds);

    List<CertDomainBinding> findByCertId(String certId);

    List<CertDomainBinding> findByCertIdIn(Collection<String> certIds);

    boolean existsByCertIdIn(Collection<String> certIds);

    long countByCertId(String certId);

    List<CertDomainBinding> findByStatusAndEnabled(BindStatus status, Boolean enabled);

    void deleteByProxyDomainIdIn(Collection<Long> proxyDomainIds);

    void deleteByCertIdIn(Collection<String> certIds);
}
