package com.xiaoniucode.etp.server.web.enums;

import lombok.Getter;

@Getter
public enum ScheduledJobCode {
    METRICS_CLEANUP("METRICS_CLEANUP", "流量统计清理", "删除超过指定天数的流量统计记录"),
    AGENT_CLEANUP("AGENT_CLEANUP", "客户端清理", "清理长期未活跃的离线客户端"),
    ACME_RENEW("ACME_RENEW", "ACME 证书续签", "在证书到期前自动续签已开启自动续签的 ACME 证书");

    private final String code;
    private final String label;
    private final String description;

    ScheduledJobCode(String code, String label, String description) {
        this.code = code;
        this.label = label;
        this.description = description;
    }

    public static ScheduledJobCode fromCode(String code) {
        for (ScheduledJobCode jobCode : values()) {
            if (jobCode.code.equals(code)) {
                return jobCode;
            }
        }
        throw new IllegalArgumentException("未知计划任务: " + code);
    }
}
