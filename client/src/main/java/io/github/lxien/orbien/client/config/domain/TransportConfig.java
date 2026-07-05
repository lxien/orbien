package io.github.lxien.orbien.client.config.domain;

import io.github.lxien.orbien.core.domain.TlsConfig;
import io.github.lxien.orbien.core.domain.transport.ProtocolListenerConfig;
import io.github.lxien.orbien.core.domain.transport.QuicProtocolConfig;
import io.github.lxien.orbien.core.domain.transport.TcpProtocolConfig;
import io.github.lxien.orbien.core.domain.transport.WebSocketProtocolConfig;
import io.github.lxien.orbien.core.transport.api.TransportEndpoint;
import io.github.lxien.orbien.core.transport.api.TransportEndpointResolver;
import io.github.lxien.orbien.core.enums.TransportProtocol;
import io.github.lxien.orbien.core.transport.tls.TlsConfigSupport;
import lombok.Data;

@Data
public class TransportConfig {
    private TransportProtocol protocol = TransportProtocol.TCP;
    private TcpProtocolConfig tcp = new TcpProtocolConfig();
    private WebSocketProtocolConfig websocket = new WebSocketProtocolConfig();
    private QuicProtocolConfig quic = new QuicProtocolConfig();
    private MultiplexConfig multiplexConfig = new MultiplexConfig(true);
    private TlsConfig tlsConfig = new TlsConfig(true);

    public TransportEndpoint resolveControlEndpoint(String serverHost, int fallbackPort) {
        syncLegacyTls();
        return TransportEndpointResolver.resolveEndpoint(serverHost, fallbackPort, protocol, tcp, websocket, quic);
    }

    public TransportEndpoint resolveDataEndpoint(String serverHost, int fallbackPort, TransportProtocol dataProtocol) {
        syncLegacyTls();
        return TransportEndpointResolver.resolveEndpoint(serverHost, fallbackPort, dataProtocol, tcp, websocket, quic);
    }

    public TlsConfig resolveTls(TransportProtocol targetProtocol) {
        syncLegacyTls();
        return TransportEndpointResolver.resolveTls(targetProtocol, tcp, websocket, quic, tlsConfig);
    }

    /**
     * 将全局 [transport.tls] 合并到各协议子段，补齐缺失的 cert/key/ca。
     */
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
