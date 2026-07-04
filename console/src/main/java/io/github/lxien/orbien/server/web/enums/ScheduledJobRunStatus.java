package io.github.lxien.orbien.server.web.enums;

import lombok.Getter;

@Getter
public enum ScheduledJobRunStatus {
    NOT_RUN(0, "未执行"),
    SUCCESS(1, "成功"),
    FAILED(2, "失败"),
    SKIPPED(3, "跳过");

    private final Integer code;
    private final String label;

    ScheduledJobRunStatus(Integer code, String label) {
        this.code = code;
        this.label = label;
    }

    public static ScheduledJobRunStatus fromCode(Integer code) {
        for (ScheduledJobRunStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("未知执行状态: " + code);
    }
}
