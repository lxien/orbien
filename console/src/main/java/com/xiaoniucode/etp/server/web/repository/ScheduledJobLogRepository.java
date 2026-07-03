package com.xiaoniucode.etp.server.web.repository;

import com.xiaoniucode.etp.server.web.entity.ScheduledJobLogDO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ScheduledJobLogRepository extends JpaRepository<ScheduledJobLogDO, Long> {

    Page<ScheduledJobLogDO> findByJobCodeOrderByStartedAtDesc(String jobCode, Pageable pageable);

    void deleteByStartedAtBefore(java.time.LocalDateTime cutoff);

    List<ScheduledJobLogDO> findByJobCodeAndIdIn(String jobCode, List<Long> ids);
}
