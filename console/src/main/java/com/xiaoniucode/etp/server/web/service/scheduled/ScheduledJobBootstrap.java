package com.xiaoniucode.etp.server.web.service.scheduled;

import com.xiaoniucode.etp.server.web.config.ScheduledJobProperties;
import com.xiaoniucode.etp.server.web.service.ScheduledJobService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ScheduledJobBootstrap {

    private static final Logger logger = LoggerFactory.getLogger(ScheduledJobBootstrap.class);

    private final ScheduledJobProperties scheduledJobProperties;
    private final ScheduledJobService scheduledJobService;
    private final DynamicJobScheduler dynamicJobScheduler;

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        if (scheduledJobProperties.isSeedOnStartup()) {
            scheduledJobService.seedJobs();
        }
        dynamicJobScheduler.refreshAll();
        logger.info("计划任务模块已启动");
    }
}
