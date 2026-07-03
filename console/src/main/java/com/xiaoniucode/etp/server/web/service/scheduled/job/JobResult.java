package com.xiaoniucode.etp.server.web.service.scheduled.job;

public record JobResult(int affectedCount, String message) {

    public static JobResult success(int affectedCount, String message) {
        return new JobResult(affectedCount, message);
    }

    public static JobResult success(String message) {
        return new JobResult(0, message);
    }
}
