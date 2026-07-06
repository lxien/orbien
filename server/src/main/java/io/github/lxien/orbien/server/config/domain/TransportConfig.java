package io.github.lxien.orbien.server.config.domain;

import io.github.lxien.orbien.core.domain.TlsConfig;
import io.github.lxien.orbien.core.domain.transport.QuicProtocolConfig;
import io.github.lxien.orbien.core.domain.transport.TcpProtocolConfig;
import io.github.lxien.orbien.core.domain.transport.WebSocketProtocolConfig;
import io.github.lxien.orbien.core.enums.TransportProtocol;
import io.github.lxien.orbien.core.transport.api.TransportEndpointResolver;
import lombok.Data;

@Data
public class TransportConfig {
    private TcpProtocolConfig tcp = new TcpProtocolConfig();
    private WebSocketProtocolConfig websocket = new WebSocketProtocolConfig();
    private QuicProtocolConfig quic = new QuicProtocolConfig();
    /**
     * TLS，TCP / WebSocket / QUIC 共享
     */
    private TlsConfig tlsConfig = new TlsConfig(true);

    public TlsConfig resolveTls(TransportProtocol protocol) {
        return TransportEndpointResolver.resolveTls(tlsConfig);
    }
}
