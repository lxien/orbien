package com.xiaoniucode.etp.server.web.repository;

import com.xiaoniucode.etp.server.web.entity.PortPoolDO;
import com.xiaoniucode.etp.server.web.enums.PortPoolType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/**
 * 端口池 Repository
 */
@Repository
public interface PortPoolRepository extends JpaRepository<PortPoolDO, Long>, JpaSpecificationExecutor<PortPoolDO> {

    boolean existsByTypeAndPortStartAndPortEnd(PortPoolType type, Integer portStart, Integer portEnd);

    boolean existsByTypeAndPortStartAndPortEndAndIdNot(PortPoolType type, Integer portStart, Integer portEnd, Long id);
}
