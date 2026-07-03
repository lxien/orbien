package com.xiaoniucode.etp.server.web.enums;

import lombok.Getter;

@Getter
public enum AcmeOrderStatus {
    DRAFT(0, "草稿"),
    PENDING_DNS(1, "待配置DNS"),
    DNS_WAITING(2, "等待DNS生效"),
    VALIDATING(3, "验证中"),
    ISSUING(4, "签发中"),
    SUCCESS(5, "已完成"),
    FAILED(6, "失败"),
    CANCELLED(7, "已取消");

    private final Integer code;
    private final String label;

    AcmeOrderStatus(Integer code, String label) {
        this.code = code;
        this.label = label;
    }

    public static AcmeOrderStatus fromCode(Integer code) {
        for (AcmeOrderStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("未知申请状态: " + code);
    }

    public boolean isTerminal() {
        return this == SUCCESS || this == FAILED || this == CANCELLED;
    }
}
