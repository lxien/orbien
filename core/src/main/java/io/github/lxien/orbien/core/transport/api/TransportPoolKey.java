package io.github.lxien.orbien.core.transport.api;
import io.github.lxien.orbien.core.enums.TransportProtocol;

import io.github.lxien.orbien.core.enums.TunnelType;

public record TransportPoolKey(TransportProtocol protocol, boolean encrypt, TunnelType tunnelType) {

    public static TransportPoolKey multiplex(TransportProtocol protocol, boolean encrypt) {
        return new TransportPoolKey(protocol, encrypt, TunnelType.MULTIPLEX);
    }

    public static TransportPoolKey direct(TransportProtocol protocol, boolean encrypt) {
        return new TransportPoolKey(protocol, encrypt, TunnelType.DIRECT);
    }
}
