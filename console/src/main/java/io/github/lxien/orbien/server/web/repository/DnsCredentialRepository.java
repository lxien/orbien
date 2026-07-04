package io.github.lxien.orbien.server.web.repository;

import io.github.lxien.orbien.server.web.entity.DnsCredentialDO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DnsCredentialRepository extends JpaRepository<DnsCredentialDO, Long> {
}
