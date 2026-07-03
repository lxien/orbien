package com.xiaoniucode.etp.server.web.entity;

import com.xiaoniucode.etp.server.web.entity.converter.ScheduledJobRunStatusConverter;
import com.xiaoniucode.etp.server.web.entity.converter.ScheduledJobTriggerTypeConverter;
import com.xiaoniucode.etp.server.web.enums.ScheduledJobRunStatus;
import com.xiaoniucode.etp.server.web.enums.ScheduledJobTriggerType;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "scheduled_job_log", indexes = {
        @Index(name = "idx_scheduled_job_log_code", columnList = "job_code"),
        @Index(name = "idx_scheduled_job_log_started", columnList = "started_at")
})
public class ScheduledJobLogDO {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "job_code", nullable = false, length = 64)
    private String jobCode;

    @Convert(converter = ScheduledJobTriggerTypeConverter.class)
    @Column(name = "trigger_type", nullable = false)
    private ScheduledJobTriggerType triggerType;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    @Convert(converter = ScheduledJobRunStatusConverter.class)
    @Column(nullable = false)
    private ScheduledJobRunStatus status;

    @Column(name = "affected_count")
    private Integer affectedCount;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
