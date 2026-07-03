package com.xiaoniucode.etp.server.web.enums;

import lombok.Getter;

@Getter
public enum DnsCredentialStatus {
    UNTESTED(0, "未测试"),
    ACTIVE(1, "正常"),
    INVALID(2, "无效");

    private final Integer code;
    private final String label;

    DnsCredentialStatus(Integer code, String label) {
        this.code = code;
        this.label = label;
    }

    public static DnsCredentialStatus fromCode(Integer code) {
        for (DnsCredentialStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("未知DNS密钥状态: " + code);
    }
}
