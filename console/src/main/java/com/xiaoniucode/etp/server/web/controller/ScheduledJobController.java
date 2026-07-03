package com.xiaoniucode.etp.server.web.controller;

import com.xiaoniucode.etp.server.web.common.message.Ajax;
import com.xiaoniucode.etp.server.web.common.message.PageQuery;
import com.xiaoniucode.etp.server.web.param.scheduled.ScheduledJobEnabledParam;
import com.xiaoniucode.etp.server.web.param.scheduled.ScheduledJobUpdateParam;
import com.xiaoniucode.etp.server.web.service.ScheduledJobService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/scheduled-jobs")
@RequiredArgsConstructor
public class ScheduledJobController {

    private final ScheduledJobService scheduledJobService;

    @GetMapping
    public Ajax list() {
        return Ajax.success(scheduledJobService.listAll());
    }

    @GetMapping("/{jobCode}")
    public Ajax detail(@PathVariable String jobCode) {
        return Ajax.success(scheduledJobService.getByCode(jobCode));
    }

    @PutMapping("/{jobCode}")
    public Ajax update(@PathVariable String jobCode, @RequestBody @Validated ScheduledJobUpdateParam param) {
        return Ajax.success(scheduledJobService.update(jobCode, param));
    }

    @PutMapping("/{jobCode}/enabled")
    public Ajax updateEnabled(@PathVariable String jobCode, @RequestBody @Validated ScheduledJobEnabledParam param) {
        return Ajax.success(scheduledJobService.updateEnabled(jobCode, Boolean.TRUE.equals(param.getEnabled())));
    }

    @PostMapping("/{jobCode}/run")
    public Ajax run(@PathVariable String jobCode) {
        scheduledJobService.runNow(jobCode);
        return Ajax.success();
    }

    @GetMapping("/{jobCode}/logs")
    public Ajax logs(@PathVariable String jobCode, @ModelAttribute PageQuery pageQuery) {
        return Ajax.success(scheduledJobService.findLogs(jobCode, pageQuery));
    }

    @DeleteMapping("/{jobCode}/logs")
    public Ajax deleteLogs(@PathVariable String jobCode, @RequestBody List<Long> ids) {
        scheduledJobService.deleteLogs(jobCode, ids);
        return Ajax.success();
    }
}
