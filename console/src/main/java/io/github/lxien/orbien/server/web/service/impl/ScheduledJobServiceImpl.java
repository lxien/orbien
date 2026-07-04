package io.github.lxien.orbien.server.web.service.impl;

import io.github.lxien.orbien.server.web.common.exception.BizException;
import io.github.lxien.orbien.server.web.common.message.PageQuery;
import io.github.lxien.orbien.server.web.common.message.PageResult;
import io.github.lxien.orbien.server.web.common.utils.JsonUtils;
import io.github.lxien.orbien.server.web.config.ScheduledJobProperties;
import io.github.lxien.orbien.server.web.dto.scheduled.ScheduledJobDTO;
import io.github.lxien.orbien.server.web.dto.scheduled.ScheduledJobLogDTO;
import io.github.lxien.orbien.server.web.entity.ScheduledJobDO;
import io.github.lxien.orbien.server.web.entity.ScheduledJobLogDO;
import io.github.lxien.orbien.server.web.enums.ScheduledJobCode;
import io.github.lxien.orbien.server.web.enums.ScheduledJobRunStatus;
import io.github.lxien.orbien.server.web.enums.ScheduledJobTriggerType;
import io.github.lxien.orbien.server.web.param.scheduled.ScheduledJobUpdateParam;
import io.github.lxien.orbien.server.web.repository.ScheduledJobLogRepository;
import io.github.lxien.orbien.server.web.repository.ScheduledJobRepository;
import io.github.lxien.orbien.server.web.service.ScheduledJobService;
import io.github.lxien.orbien.server.web.service.scheduled.DynamicJobScheduler;
import io.github.lxien.orbien.server.web.service.scheduled.ScheduledJobEnableSupport;
import io.github.lxien.orbien.server.web.service.scheduled.ScheduledJobSchemaProvider;
import io.github.lxien.orbien.server.web.service.scheduled.job.JobResult;
import io.github.lxien.orbien.server.web.service.scheduled.job.ScheduledJobHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ScheduledJobServiceImpl implements ScheduledJobService {

    private static final Logger logger = LoggerFactory.getLogger(ScheduledJobServiceImpl.class);

    private final ScheduledJobRepository scheduledJobRepository;
    private final ScheduledJobLogRepository scheduledJobLogRepository;
    private final ScheduledJobSchemaProvider schemaProvider;
    private final DynamicJobScheduler dynamicJobScheduler;
    private final ScheduledJobEnableSupport scheduledJobEnableSupport;
    private final ScheduledJobProperties scheduledJobProperties;
    private final Map<ScheduledJobCode, ScheduledJobHandler> handlerMap;
    private final ConcurrentHashMap<String, Boolean> runningFlags = new ConcurrentHashMap<>();

    public ScheduledJobServiceImpl(
            ScheduledJobRepository scheduledJobRepository,
            ScheduledJobLogRepository scheduledJobLogRepository,
            ScheduledJobSchemaProvider schemaProvider,
            DynamicJobScheduler dynamicJobScheduler,
            ScheduledJobEnableSupport scheduledJobEnableSupport,
            ScheduledJobProperties scheduledJobProperties,
            List<ScheduledJobHandler> handlers) {
        this.scheduledJobRepository = scheduledJobRepository;
        this.scheduledJobLogRepository = scheduledJobLogRepository;
        this.schemaProvider = schemaProvider;
        this.dynamicJobScheduler = dynamicJobScheduler;
        this.scheduledJobEnableSupport = scheduledJobEnableSupport;
        this.scheduledJobProperties = scheduledJobProperties;
        this.handlerMap = handlers.stream().collect(Collectors.toMap(ScheduledJobHandler::jobCode, Function.identity()));
    }

    @Override
    public List<ScheduledJobDTO> listAll() {
        return scheduledJobRepository.findAll().stream()
                .sorted((a, b) -> a.getJobCode().compareTo(b.getJobCode()))
                .map(this::toDTO)
                .toList();
    }

    @Override
    public ScheduledJobDTO getByCode(String jobCode) {
        return toDTO(requireJob(jobCode));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ScheduledJobDTO update(String jobCode, ScheduledJobUpdateParam param) {
        validateCron(param.getCronExpression());
        ScheduledJobCode code = ScheduledJobCode.fromCode(jobCode);
        validateParams(code, param.getParams());

        ScheduledJobDO job = requireJob(jobCode);
        job.setCronExpression(param.getCronExpression());
        job.setParamsJson(JsonUtils.toJson(param.getParams()));
        if (Boolean.TRUE.equals(job.getEnabled())) {
            job.setNextRunAt(dynamicJobScheduler.previewNextRun(param.getCronExpression()));
        }
        scheduledJobRepository.save(job);
        dynamicJobScheduler.refreshJob(jobCode);
        return toDTO(job);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ScheduledJobDTO updateEnabled(String jobCode, boolean enabled) {
        ScheduledJobDO job = requireJob(jobCode);
        job.setEnabled(enabled);
        job.setNextRunAt(enabled ? dynamicJobScheduler.previewNextRun(job.getCronExpression()) : null);
        scheduledJobRepository.save(job);
        dynamicJobScheduler.refreshJob(jobCode);
        return toDTO(job);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean enableIfDisabled(String jobCode) {
        return scheduledJobEnableSupport.enableIfDisabled(jobCode);
    }

    @Override
    public void runNow(String jobCode) {
        requireJob(jobCode);
        executeJob(jobCode, true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void executeJob(String jobCode, boolean manual) {
        if (runningFlags.putIfAbsent(jobCode, Boolean.TRUE) != null) {
            logger.warn("计划任务正在执行，跳过: jobCode={}", jobCode);
            return;
        }
        try {
            doExecuteJob(jobCode, manual);
        } finally {
            runningFlags.remove(jobCode);
        }
    }

    private void doExecuteJob(String jobCode, boolean manual) {
        ScheduledJobDO job = requireJob(jobCode);
        if (!manual && !Boolean.TRUE.equals(job.getEnabled())) {
            writeLog(jobCode, manual, ScheduledJobRunStatus.SKIPPED, 0, "任务未启用", LocalDateTime.now());
            updateJobStatus(job, ScheduledJobRunStatus.SKIPPED, "任务未启用", LocalDateTime.now());
            return;
        }

        ScheduledJobCode code = ScheduledJobCode.fromCode(jobCode);
        ScheduledJobHandler handler = handlerMap.get(code);
        if (handler == null) {
            writeLog(jobCode, manual, ScheduledJobRunStatus.FAILED, 0, "未找到任务处理器", LocalDateTime.now());
            updateJobStatus(job, ScheduledJobRunStatus.FAILED, "未找到任务处理器", LocalDateTime.now());
            return;
        }

        LocalDateTime startedAt = LocalDateTime.now();
        try {
            JobResult result = handler.execute(job.getParamsJson());
            writeLog(jobCode, manual, ScheduledJobRunStatus.SUCCESS, result.affectedCount(), result.message(), startedAt);
            updateJobStatus(job, ScheduledJobRunStatus.SUCCESS, result.message(), startedAt);
            logger.info("计划任务执行成功: jobCode={}, affected={}, message={}",
                    jobCode, result.affectedCount(), result.message());
        } catch (Exception e) {
            logger.error("计划任务执行失败: jobCode={}", jobCode, e);
            writeLog(jobCode, manual, ScheduledJobRunStatus.FAILED, 0, e.getMessage(), startedAt);
            updateJobStatus(job, ScheduledJobRunStatus.FAILED, e.getMessage(), startedAt);
        }
        cleanupOldLogs();
    }

    @Override
    public PageResult<ScheduledJobLogDTO> findLogs(String jobCode, PageQuery pageQuery) {
        requireJob(jobCode);
        int currentPage = Math.max(0, pageQuery.getCurrent() - 1);
        Pageable pageable = PageRequest.of(currentPage, pageQuery.getSize(), Sort.by(Sort.Direction.DESC, "startedAt"));
        Page<ScheduledJobLogDO> page = scheduledJobLogRepository.findByJobCodeOrderByStartedAtDesc(jobCode, pageable);
        if (page.isEmpty()) {
            return PageResult.empty(pageQuery.getCurrent(), pageQuery.getSize());
        }
        return PageResult.wrap(page, page.getContent().stream().map(this::toLogDTO).toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteLogs(String jobCode, List<Long> ids) {
        requireJob(jobCode);
        if (ids == null || ids.isEmpty()) {
            throw new BizException("请选择要删除的日志");
        }
        List<Long> distinctIds = ids.stream().distinct().toList();
        List<ScheduledJobLogDO> logs = scheduledJobLogRepository.findByJobCodeAndIdIn(jobCode, distinctIds);
        if (logs.size() != distinctIds.size()) {
            throw new BizException("部分日志不存在或不属于当前任务");
        }
        scheduledJobLogRepository.deleteAll(logs);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void seedJobs() {
        seedOne(ScheduledJobCode.METRICS_CLEANUP, scheduledJobProperties.getMetricsCleanup(),
                Map.of("retentionDays", 90));
        seedOne(ScheduledJobCode.AGENT_CLEANUP, scheduledJobProperties.getAgentCleanup(),
                Map.of("inactiveDays", 30, "onlyOffline", true, "excludeWithProxy", true));
        seedOne(ScheduledJobCode.ACME_RENEW, scheduledJobProperties.getAcmeRenew(),
                Map.of("renewBeforeDays", 30, "onlyAcmeSource", true, "respectCertAutoRenew", true, "maxCertsPerRun", 20));
    }

    private void seedOne(ScheduledJobCode code, ScheduledJobProperties.JobDefaults defaults, Map<String, Object> params) {
        if (scheduledJobRepository.existsByJobCode(code.getCode())) {
            return;
        }
        ScheduledJobDO job = new ScheduledJobDO();
        job.setJobCode(code.getCode());
        job.setJobName(code.getLabel());
        job.setDescription(code.getDescription());
        job.setEnabled(defaults.isEnabled());
        job.setCronExpression(defaults.getCron());
        job.setParamsJson(JsonUtils.toJson(params));
        job.setLastRunStatus(ScheduledJobRunStatus.NOT_RUN);
        job.setNextRunAt(defaults.isEnabled() ? dynamicJobScheduler.previewNextRun(defaults.getCron()) : null);
        scheduledJobRepository.save(job);
    }

    private void validateCron(String cronExpression) {
        try {
            CronExpression.parse(cronExpression);
        } catch (Exception e) {
            throw new BizException("Cron 表达式无效");
        }
    }

    private void validateParams(ScheduledJobCode code, Map<String, Object> params) {
        schemaProvider.schema(code).forEach(field -> {
            if (!field.isRequired()) {
                return;
            }
            Object value = params.get(field.getKey());
            if (value == null) {
                throw new BizException("参数缺失: " + field.getLabel());
            }
        });
    }

    private ScheduledJobDO requireJob(String jobCode) {
        return scheduledJobRepository.findByJobCode(jobCode)
                .orElseThrow(() -> new BizException("计划任务不存在"));
    }

    private void updateJobStatus(ScheduledJobDO job, ScheduledJobRunStatus status, String message, LocalDateTime startedAt) {
        job.setLastRunAt(startedAt);
        job.setLastRunStatus(status);
        job.setLastRunMessage(message);
        if (Boolean.TRUE.equals(job.getEnabled())) {
            job.setNextRunAt(dynamicJobScheduler.previewNextRun(job.getCronExpression()));
        }
        scheduledJobRepository.save(job);
    }

    private void writeLog(
            String jobCode,
            boolean manual,
            ScheduledJobRunStatus status,
            int affectedCount,
            String message,
            LocalDateTime startedAt) {
        ScheduledJobLogDO log = new ScheduledJobLogDO();
        log.setJobCode(jobCode);
        log.setTriggerType(manual ? ScheduledJobTriggerType.MANUAL : ScheduledJobTriggerType.SCHEDULED);
        log.setStartedAt(startedAt);
        log.setFinishedAt(LocalDateTime.now());
        log.setStatus(status);
        log.setAffectedCount(affectedCount);
        log.setMessage(message);
        log.setCreatedAt(startedAt);
        scheduledJobLogRepository.save(log);
    }

    private void cleanupOldLogs() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(scheduledJobProperties.getLogRetentionDays());
        scheduledJobLogRepository.deleteByStartedAtBefore(cutoff);
    }

    private ScheduledJobDTO toDTO(ScheduledJobDO job) {
        ScheduledJobDTO dto = new ScheduledJobDTO();
        dto.setJobCode(job.getJobCode());
        dto.setJobName(job.getJobName());
        dto.setDescription(job.getDescription());
        dto.setEnabled(job.getEnabled());
        dto.setCronExpression(job.getCronExpression());
        dto.setParams(JsonUtils.toMap(job.getParamsJson()));
        ScheduledJobCode code = ScheduledJobCode.fromCode(job.getJobCode());
        dto.setParamSchema(schemaProvider.schema(code));
        dto.setLastRunAt(job.getLastRunAt());
        if (job.getLastRunStatus() != null) {
            dto.setLastRunStatus(job.getLastRunStatus().getCode());
            dto.setLastRunStatusLabel(job.getLastRunStatus().getLabel());
        }
        dto.setLastRunMessage(job.getLastRunMessage());
        dto.setNextRunAt(job.getNextRunAt());
        return dto;
    }

    private ScheduledJobLogDTO toLogDTO(ScheduledJobLogDO log) {
        ScheduledJobLogDTO dto = new ScheduledJobLogDTO();
        dto.setId(log.getId());
        dto.setJobCode(log.getJobCode());
        dto.setTriggerType(log.getTriggerType().getCode());
        dto.setTriggerTypeLabel(log.getTriggerType().getLabel());
        dto.setStartedAt(log.getStartedAt());
        dto.setFinishedAt(log.getFinishedAt());
        dto.setStatus(log.getStatus().getCode());
        dto.setStatusLabel(log.getStatus().getLabel());
        dto.setAffectedCount(log.getAffectedCount());
        dto.setMessage(log.getMessage());
        return dto;
    }
}
