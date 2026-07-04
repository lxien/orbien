package io.github.lxien.orbien.server.web.enums;

import lombok.Getter;

@Getter
public enum AcmeChallengeStatus {
    PENDING(1, "待验证"),
    PROPAGATING(2, "传播中"),
    VALIDATED(3, "已验证"),
    FAILED(4, "失败"),
    CLEANED(5, "已清理");

    private final Integer code;
    private final String label;

    AcmeChallengeStatus(Integer code, String label) {
        this.code = code;
        this.label = label;
    }

    public static AcmeChallengeStatus fromCode(Integer code) {
        for (AcmeChallengeStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("未知挑战状态: " + code);
    }
}
