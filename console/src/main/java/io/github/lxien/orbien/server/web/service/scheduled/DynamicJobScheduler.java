package io.github.lxien.orbien.server.web.service.scheduled;

import io.github.lxien.orbien.server.web.repository.ScheduledJobRepository;
import io.github.lxien.orbien.server.web.service.ScheduledJobService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

@Component
public class DynamicJobScheduler {

    private static final Logger logger = LoggerFactory.getLogger(DynamicJobScheduler.class);

    private final TaskScheduler taskScheduler;
    private final ScheduledJobRepository scheduledJobRepository;
    private final ScheduledJobService scheduledJobService;
    private final Map<String, ScheduledFuture<?>> futures = new ConcurrentHashMap<>();

    public DynamicJobScheduler(
            TaskScheduler taskScheduler,
            ScheduledJobRepository scheduledJobRepository,
            @Lazy ScheduledJobService scheduledJobService) {
        this.taskScheduler = taskScheduler;
        this.scheduledJobRepository = scheduledJobRepository;
        this.scheduledJobService = scheduledJobService;
    }

    public void refreshAll() {
        scheduledJobRepository.findAll().forEach(job -> refreshJob(job.getJobCode()));
    }

    public void refreshJob(String jobCode) {
        cancel(jobCode);
        scheduledJobRepository.findByJobCode(jobCode).ifPresent(job -> {
            if (!Boolean.TRUE.equals(job.getEnabled())) {
                job.setNextRunAt(null);
                scheduledJobRepository.save(job);
                return;
            }
            try {
                CronExpression expression = CronExpression.parse(job.getCronExpression());
                LocalDateTime next = expression.next(LocalDateTime.now());
                job.setNextRunAt(next);
                scheduledJobRepository.save(job);
                ScheduledFuture<?> future = taskScheduler.schedule(
                        () -> scheduledJobService.executeJob(jobCode, false),
                        new CronTrigger(job.getCronExpression()));
                futures.put(jobCode, future);
                logger.info("计划任务已注册: jobCode={}, cron={}, nextRunAt={}", jobCode, job.getCronExpression(), next);
            } catch (Exception e) {
                logger.error("计划任务注册失败: jobCode={}", jobCode, e);
            }
        });
    }

    public void cancel(String jobCode) {
        ScheduledFuture<?> future = futures.remove(jobCode);
        if (future != null) {
            future.cancel(false);
        }
    }

    public LocalDateTime previewNextRun(String cronExpression) {
        return CronExpression.parse(cronExpression).next(LocalDateTime.now());
    }
}
