package io.github.lxien.orbien.server.web.service.scheduled.job;

import lombok.Data;

@Data
public class MetricsCleanupJobParams {
    private int retentionDays = 90;
}
