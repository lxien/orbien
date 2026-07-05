package io.github.lxien.orbien.core.transport.api;

import io.github.lxien.orbien.core.domain.TlsConfig;
import io.github.lxien.orbien.core.domain.TransportCustomConfig;
import io.github.lxien.orbien.core.enums.TransportProtocol;
import io.github.lxien.orbien.core.domain.transport.QuicProtocolConfig;
import io.github.lxien.orbien.core.domain.transport.TcpProtocolConfig;
import io.github.lxien.orbien.core.domain.transport.WebSocketProtocolConfig;
import io.github.lxien.orbien.core.transport.tls.TlsConfigSupport;

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
                                                      TcpProtocolConfig tcp,
                                                      WebSocketProtocolConfig websocket,
                                                      QuicProtocolConfig quic) {
        return switch (protocol) {
            case TCP -> TransportEndpoint.builder()
                    .host(serverHost)
                    .port(tcp != null && tcp.getPort() > 0 ? tcp.getPort() : fallbackPort)
                    .protocol(TransportProtocol.TCP)
                    .tlsConfig(tcp != null ? tcp.getTlsConfig() : null)
                    .build();
            case WEBSOCKET -> TransportEndpoint.builder()
                    .host(serverHost)
                    .port(websocket != null ? websocket.getPort() : TransportProtocol.WEBSOCKET.getDefaultPort())
                    .protocol(TransportProtocol.WEBSOCKET)
                    .tlsConfig(websocket != null ? websocket.getTlsConfig() : null)
                    .webSocketPath(websocket != null ? websocket.getPath() : "/tunnel")
                    .build();
            case QUIC -> TransportEndpoint.builder()
                    .host(serverHost)
                    .port(quic != null ? quic.getPort() : TransportProtocol.QUIC.getDefaultPort())
                    .protocol(TransportProtocol.QUIC)
                    .tlsConfig(quic != null ? quic.getTlsConfig() : null)
                    .build();
        };
    }

    public static boolean normalizeMultiplex(TransportProtocol protocol, boolean multiplex) {
        if (protocol == TransportProtocol.QUIC) {
            return true;
        }
        return multiplex;
    }

    public static TlsConfig resolveTls(TransportProtocol protocol,
                                       TcpProtocolConfig tcp,
                                       WebSocketProtocolConfig websocket,
                                       QuicProtocolConfig quic,
                                       TlsConfig legacyTls) {
        TlsConfig tls = switch (protocol) {
            case TCP -> tcp != null ? tcp.getTlsConfig() : legacyTls;
            case WEBSOCKET -> websocket != null ? websocket.getTlsConfig() : legacyTls;
            case QUIC -> quic != null ? quic.getTlsConfig() : legacyTls;
        };
        return TlsConfigSupport.effective(tls, legacyTls);
    }
}
