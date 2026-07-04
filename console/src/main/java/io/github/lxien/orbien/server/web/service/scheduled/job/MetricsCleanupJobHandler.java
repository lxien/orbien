package io.github.lxien.orbien.server.web.service.scheduled.job;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.lxien.orbien.server.web.enums.ScheduledJobCode;
import io.github.lxien.orbien.server.web.service.MetricsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MetricsCleanupJobHandler implements ScheduledJobHandler {

    private final MetricsService metricsService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public ScheduledJobCode jobCode() {
        return ScheduledJobCode.METRICS_CLEANUP;
    }

    @Override
    public JobResult execute(String paramsJson) {
        MetricsCleanupJobParams params = parseParams(paramsJson);
        int deleted = metricsService.deleteOldMetrics(params.getRetentionDays());
        return JobResult.success(deleted, "已清理 " + deleted + " 条流量统计记录");
    }

    private MetricsCleanupJobParams parseParams(String paramsJson) {
        try {
            if (paramsJson == null || paramsJson.isBlank()) {
                return new MetricsCleanupJobParams();
            }
            return objectMapper.readValue(paramsJson, MetricsCleanupJobParams.class);
        } catch (Exception e) {
            return new MetricsCleanupJobParams();
        }
    }
}
