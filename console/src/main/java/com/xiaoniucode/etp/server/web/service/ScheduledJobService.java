package com.xiaoniucode.etp.server.web.service;

import com.xiaoniucode.etp.server.web.common.message.PageQuery;
import com.xiaoniucode.etp.server.web.common.message.PageResult;
import com.xiaoniucode.etp.server.web.dto.scheduled.ScheduledJobDTO;
import com.xiaoniucode.etp.server.web.dto.scheduled.ScheduledJobLogDTO;
import com.xiaoniucode.etp.server.web.param.scheduled.ScheduledJobUpdateParam;

import java.util.List;

public interface ScheduledJobService {

    List<ScheduledJobDTO> listAll();

    ScheduledJobDTO getByCode(String jobCode);

    ScheduledJobDTO update(String jobCode, ScheduledJobUpdateParam param);

    ScheduledJobDTO updateEnabled(String jobCode, boolean enabled);

    /**
     * 若任务当前为停用状态则启用，用于证书自动续签等联动场景。
     *
     * @return 是否在本次调用中启用了任务
     */
    boolean enableIfDisabled(String jobCode);

    void runNow(String jobCode);

    void executeJob(String jobCode, boolean manual);

    PageResult<ScheduledJobLogDTO> findLogs(String jobCode, PageQuery pageQuery);

    void deleteLogs(String jobCode, List<Long> ids);

    void seedJobs();
}
