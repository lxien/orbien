package com.xiaoniucode.etp.server.web.service.scheduled.job;

import lombok.Data;

@Data
public class AgentCleanupJobParams {
    private int inactiveDays = 30;
    private boolean onlyOffline = true;
    private boolean excludeWithProxy = true;
}
