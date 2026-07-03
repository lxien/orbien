package com.xiaoniucode.etp.server.web.enums;

import lombok.Getter;

@Getter
public enum ScheduledJobTriggerType {
    SCHEDULED(1, "定时"),
    MANUAL(2, "手动");

    private final Integer code;
    private final String label;

    ScheduledJobTriggerType(Integer code, String label) {
        this.code = code;
        this.label = label;
    }

    public static ScheduledJobTriggerType fromCode(Integer code) {
        for (ScheduledJobTriggerType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("未知触发类型: " + code);
    }
}
