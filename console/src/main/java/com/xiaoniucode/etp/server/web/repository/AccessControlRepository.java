package com.xiaoniucode.etp.server.web.repository;
import com.xiaoniucode.etp.server.web.entity.AccessControlDO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 访问控制 Repository
 */
@Repository
public interface AccessControlRepository extends JpaRepository<AccessControlDO, String> {
    void deleteByProxyIdIn(List<String> ids);
}
