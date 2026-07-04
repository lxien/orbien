package io.github.lxien.orbien.core.enums;

import lombok.Getter;

@Getter
public enum ProxyStatus {
    CLOSED(0, "关闭"),
    OPEN(1, "开启");

    private final Integer code;
    private final String description;

    ProxyStatus(Integer code, String description) {
        this.code = code;
        this.description = description;
    }

    public static ProxyStatus fromCode(Integer code) {
        for (ProxyStatus proxyStatus : values()) {
            if (proxyStatus.getCode().equals(code)) {
                return proxyStatus;
            }
        }
        return null;
    }

    public boolean isOpen() {
        return this == OPEN;
    }

    public boolean isClosed() {
        return this == CLOSED;
    }
}
