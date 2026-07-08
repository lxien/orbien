package io.github.lxien.orbien.autoconfigure;

import io.github.lxien.orbien.core.enums.ProtocolType;

public enum WebProxyProtocol {
    HTTP,
    HTTPS;
    ProtocolType toProtocolType() {
        return this == HTTPS ? ProtocolType.HTTPS : ProtocolType.HTTP;
    }
}
