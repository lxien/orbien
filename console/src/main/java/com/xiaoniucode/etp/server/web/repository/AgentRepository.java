package com.xiaoniucode.etp.server.web.repository;

import com.xiaoniucode.etp.server.web.entity.AgentDO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 客户端 Repository
 */
@Repository
public interface AgentRepository extends JpaRepository<AgentDO, String> {

}
