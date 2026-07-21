package io.github.lxien.orbien.autoconfigure;

import io.github.lxien.orbien.core.enums.ProtocolType;

public enum WebProxyProtocol {
    HTTP,
    HTTPS,
    TCP;

    ProtocolType toProtocolType() {
        switch (this) {
            case HTTPS:
                return ProtocolType.HTTPS;
            case TCP:
                return ProtocolType.TCP;
            case HTTP:
            default:
                return ProtocolType.HTTP;
        }
    }

    boolean isHttpOrHttps() {
        return this == HTTP || this == HTTPS;
    }
}
