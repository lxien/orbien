package io.github.lxien.orbien.server.web.repository;

import io.github.lxien.orbien.server.web.entity.AcmeCertOrderDO;
import io.github.lxien.orbien.server.web.enums.AcmeOrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AcmeCertOrderRepository extends JpaRepository<AcmeCertOrderDO, Long> {

    boolean existsByDnsCredentialIdAndStatusIn(Long dnsCredentialId, List<AcmeOrderStatus> statuses);

    boolean existsByCertIdAndStatusIn(String certId, List<AcmeOrderStatus> statuses);

    Optional<AcmeCertOrderDO> findFirstByCertIdAndStatusOrderByCreatedAtDesc(String certId, AcmeOrderStatus status);

    Page<AcmeCertOrderDO> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
