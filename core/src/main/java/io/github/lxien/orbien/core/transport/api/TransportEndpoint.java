package io.github.lxien.orbien.core.transport.api;
import io.github.lxien.orbien.core.enums.TransportProtocol;

import io.github.lxien.orbien.core.domain.TlsConfig;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TransportEndpoint {
    private final String host;
    private final int port;
    private final TransportProtocol protocol;
    private final TlsConfig tlsConfig;
    private final String webSocketPath;
}
