package io.github.lxien.orbien.server.web.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "orbien.scheduled-job")
public class ScheduledJobProperties {
    private boolean seedOnStartup = true;
    private JobDefaults metricsCleanup = new JobDefaults("0 0 1 * * ?", true);
    private JobDefaults agentCleanup = new JobDefaults("0 30 2 * * ?", false);
    private JobDefaults acmeRenew = new JobDefaults("0 0 3 * * ?", true);
    private int logRetentionDays = 30;

    @Data
    public static class JobDefaults {
        private String cron;
        private boolean enabled;

        public JobDefaults() {
        }

        public JobDefaults(String cron, boolean enabled) {
            this.cron = cron;
            this.enabled = enabled;
        }
    }
}
