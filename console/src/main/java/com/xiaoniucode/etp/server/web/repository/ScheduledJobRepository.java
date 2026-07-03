package com.xiaoniucode.etp.server.web.repository;

import com.xiaoniucode.etp.server.web.entity.ScheduledJobDO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ScheduledJobRepository extends JpaRepository<ScheduledJobDO, Long> {

    Optional<ScheduledJobDO> findByJobCode(String jobCode);

    boolean existsByJobCode(String jobCode);
}
