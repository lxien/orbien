package io.github.lxien.orbien.core.transport.api;

import io.github.lxien.orbien.core.domain.TlsConfig;
import io.github.lxien.orbien.core.domain.TransportCustomConfig;
import io.github.lxien.orbien.core.domain.transport.ProtocolListenerConfig;
import io.github.lxien.orbien.core.domain.transport.QuicProtocolConfig;
import io.github.lxien.orbien.core.domain.transport.WebSocketProtocolConfig;
import io.github.lxien.orbien.core.enums.TransportProtocol;

public final class TransportEndpointResolver {

    private TransportEndpointResolver() {
    }

    public static TransportProtocol resolveControlProtocol(TransportProtocol configured) {
        return configured != null ? configured : TransportProtocol.TCP;
    }

    public static TransportProtocol resolveDataProtocol(TransportProtocol globalDefault,
                                                        TransportCustomConfig proxyTransport) {
        if (proxyTransport != null && proxyTransport.getProtocol() != null) {
            return proxyTransport.getProtocol();
        }
        return globalDefault != null ? globalDefault : TransportProtocol.TCP;
    }

    public static TransportEndpoint resolveEndpoint(String serverHost,
                                                      int fallbackPort,
                                                      TransportProtocol protocol,
                                                      WebSocketProtocolConfig websocket,
                                                      QuicProtocolConfig quic,
                                                      TlsConfig tlsConfig) {
        return switch (protocol) {
            case TCP -> TransportEndpoint.builder()
                    .host(serverHost)
                    .port(fallbackPort)
                    .protocol(TransportProtocol.TCP)
                    .tlsConfig(tlsConfig)
                    .build();
            case WEBSOCKET -> TransportEndpoint.builder()
                    .host(serverHost)
                    .port(resolvePort(websocket, TransportProtocol.WEBSOCKET.getDefaultPort()))
                    .protocol(TransportProtocol.WEBSOCKET)
                    .tlsConfig(tlsConfig)
                    .webSocketPath(websocket != null ? websocket.getPath() : "/tunnel")
                    .build();
            case QUIC -> TransportEndpoint.builder()
                    .host(serverHost)
                    .port(resolvePort(quic, TransportProtocol.QUIC.getDefaultPort()))
                    .protocol(TransportProtocol.QUIC)
                    .tlsConfig(tlsConfig)
                    .build();
        };
    }

    public static boolean normalizeMultiplex(TransportProtocol protocol, boolean multiplex) {
        if (protocol == TransportProtocol.QUIC) {
            return true;
        }
        return multiplex;
    }

    public static TlsConfig resolveTls(TlsConfig sharedTls) {
        return sharedTls;
    }

    private static int resolvePort(ProtocolListenerConfig config, int defaultPort) {
        if (config != null && config.getPort() > 0) {
            return config.getPort();
        }
        return defaultPort;
    }
}
