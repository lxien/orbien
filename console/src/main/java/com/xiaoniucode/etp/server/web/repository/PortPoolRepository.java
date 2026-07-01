package com.xiaoniucode.etp.server.web.repository;

import com.xiaoniucode.etp.server.web.entity.PortPoolDO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/**
 * 端口池 Repository
 */
@Repository
public interface PortPoolRepository extends JpaRepository<PortPoolDO, Long>, JpaSpecificationExecutor<PortPoolDO> {
}