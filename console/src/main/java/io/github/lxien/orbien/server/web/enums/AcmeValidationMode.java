package io.github.lxien.orbien.server.web.enums;

import lombok.Getter;

@Getter
public enum AcmeValidationMode {
    MANUAL(1, "手动DNS"),
    DNS_API(2, "云DNS自动");

    private final Integer code;
    private final String label;

    AcmeValidationMode(Integer code, String label) {
        this.code = code;
        this.label = label;
    }

    public static AcmeValidationMode fromCode(Integer code) {
        for (AcmeValidationMode mode : values()) {
            if (mode.code.equals(code)) {
                return mode;
            }
        }
        throw new IllegalArgumentException("未知验证方式: " + code);
    }
}
