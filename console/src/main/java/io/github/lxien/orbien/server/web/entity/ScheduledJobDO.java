package io.github.lxien.orbien.server.web.entity;

import io.github.lxien.orbien.server.web.entity.converter.ScheduledJobRunStatusConverter;
import io.github.lxien.orbien.server.web.enums.ScheduledJobRunStatus;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "scheduled_job")
public class ScheduledJobDO {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "job_code", nullable = false, unique = true, length = 64)
    private String jobCode;

    @Column(name = "job_name", nullable = false, length = 128)
    private String jobName;

    @Column(length = 512)
    private String description;

    @Column(nullable = false)
    private Boolean enabled = true;

    @Column(name = "cron_expression", nullable = false, length = 64)
    private String cronExpression;

    @Column(name = "params_json", columnDefinition = "TEXT")
    private String paramsJson;

    @Column(name = "last_run_at")
    private LocalDateTime lastRunAt;

    @Convert(converter = ScheduledJobRunStatusConverter.class)
    @Column(name = "last_run_status")
    private ScheduledJobRunStatus lastRunStatus = ScheduledJobRunStatus.NOT_RUN;

    @Column(name = "last_run_message", length = 512)
    private String lastRunMessage;

    @Column(name = "next_run_at")
    private LocalDateTime nextRunAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
