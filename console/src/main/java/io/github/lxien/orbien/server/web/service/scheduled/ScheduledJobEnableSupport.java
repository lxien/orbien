package io.github.lxien.orbien.server.web.service.scheduled;

import io.github.lxien.orbien.server.web.common.exception.BizException;
import io.github.lxien.orbien.server.web.entity.ScheduledJobDO;
import io.github.lxien.orbien.server.web.repository.ScheduledJobRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 计划任务启用/停用辅助类，仅依赖仓储与调度器，供 SSL 续签联动等场景使用，避免与任务执行链循环依赖。
 */
@Component
public class ScheduledJobEnableSupport {

    private static final Logger logger = LoggerFactory.getLogger(ScheduledJobEnableSupport.class);

    private final ScheduledJobRepository scheduledJobRepository;
    private final DynamicJobScheduler dynamicJobScheduler;

    public ScheduledJobEnableSupport(
            ScheduledJobRepository scheduledJobRepository,
            DynamicJobScheduler dynamicJobScheduler) {
        this.scheduledJobRepository = scheduledJobRepository;
        this.dynamicJobScheduler = dynamicJobScheduler;
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean enableIfDisabled(String jobCode) {
        ScheduledJobDO job = scheduledJobRepository.findByJobCode(jobCode)
                .orElseThrow(() -> new BizException("计划任务不存在"));
        if (Boolean.TRUE.equals(job.getEnabled())) {
            return false;
        }
        job.setEnabled(true);
        job.setNextRunAt(dynamicJobScheduler.previewNextRun(job.getCronExpression()));
        scheduledJobRepository.save(job);
        dynamicJobScheduler.refreshJob(jobCode);
        logger.info("计划任务已联动启用: jobCode={}", jobCode);
        return true;
    }
}
