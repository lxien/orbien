package io.github.lxien.orbien.server.web.service.scheduled.job;

import io.github.lxien.orbien.server.web.enums.ScheduledJobCode;

public interface ScheduledJobHandler {

    ScheduledJobCode jobCode();

    JobResult execute(String paramsJson);
}
