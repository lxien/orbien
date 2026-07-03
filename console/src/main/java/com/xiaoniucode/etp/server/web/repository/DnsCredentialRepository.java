package com.xiaoniucode.etp.server.web.repository;

import com.xiaoniucode.etp.server.web.entity.DnsCredentialDO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DnsCredentialRepository extends JpaRepository<DnsCredentialDO, Long> {
}
