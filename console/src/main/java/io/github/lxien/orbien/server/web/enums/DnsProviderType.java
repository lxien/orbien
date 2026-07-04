package io.github.lxien.orbien.server.web.enums;

import lombok.Getter;

@Getter
public enum DnsProviderType {
    ALIYUN(1, "阿里云"),
    TENCENT(2, "腾讯云"),
    CLOUDFLARE(3, "Cloudflare");

    private final Integer code;
    private final String label;

    DnsProviderType(Integer code, String label) {
        this.code = code;
        this.label = label;
    }

    public static DnsProviderType fromCode(Integer code) {
        for (DnsProviderType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("未知DNS厂商: " + code);
    }
}
