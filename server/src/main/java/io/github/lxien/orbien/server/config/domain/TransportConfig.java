package io.github.lxien.orbien.server.config.domain;

import io.github.lxien.orbien.core.domain.TlsConfig;
import io.github.lxien.orbien.core.domain.transport.ProtocolListenerConfig;
import io.github.lxien.orbien.core.domain.transport.QuicProtocolConfig;
import io.github.lxien.orbien.core.domain.transport.TcpProtocolConfig;
import io.github.lxien.orbien.core.domain.transport.WebSocketProtocolConfig;
import io.github.lxien.orbien.core.enums.TransportProtocol;
import io.github.lxien.orbien.core.transport.api.TransportEndpointResolver;
import io.github.lxien.orbien.core.transport.tls.TlsConfigSupport;
import lombok.Data;

@Data
public class TransportConfig {
    private TcpProtocolConfig tcp = new TcpProtocolConfig();
    private WebSocketProtocolConfig websocket = new WebSocketProtocolConfig();
    private QuicProtocolConfig quic = new QuicProtocolConfig();
    private TlsConfig tlsConfig = new TlsConfig(true);

    public TlsConfig resolveTls(TransportProtocol protocol) {
        syncLegacyTls();
        return TransportEndpointResolver.resolveTls(protocol, tcp, websocket, quic, tlsConfig);
    }

    public void syncLegacyTls() {
        if (tlsConfig == null) {
            return;
        }
        propagateTls(tcp, tlsConfig);
        propagateTls(websocket, tlsConfig);
        propagateTls(quic, tlsConfig);
    }

    private void propagateTls(ProtocolListenerConfig target, TlsConfig legacy) {
        target.setTlsConfig(TlsConfigSupport.merge(legacy, target.getTlsConfig()));
    }
}
