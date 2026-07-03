package com.xiaoniucode.etp.server.web.service.scheduled.job;

import com.xiaoniucode.etp.server.web.enums.ScheduledJobCode;

public interface ScheduledJobHandler {

    ScheduledJobCode jobCode();

    JobResult execute(String paramsJson);
}
