package io.github.lxien.orbien.client.config.domain;

import io.github.lxien.orbien.core.domain.TlsConfig;
import io.github.lxien.orbien.core.domain.transport.QuicProtocolConfig;
import io.github.lxien.orbien.core.domain.transport.TcpProtocolConfig;
import io.github.lxien.orbien.core.domain.transport.WebSocketProtocolConfig;
import io.github.lxien.orbien.core.transport.api.TransportEndpoint;
import io.github.lxien.orbien.core.transport.api.TransportEndpointResolver;
import io.github.lxien.orbien.core.enums.TransportProtocol;
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
        return TransportEndpointResolver.resolveEndpoint(
                serverHost, fallbackPort, protocol, websocket, quic, tlsConfig);
    }

    public TransportEndpoint resolveDataEndpoint(String serverHost, int fallbackPort, TransportProtocol dataProtocol) {
        return TransportEndpointResolver.resolveEndpoint(
                serverHost, fallbackPort, dataProtocol, websocket, quic, tlsConfig);
    }

    public TlsConfig resolveTls(TransportProtocol targetProtocol) {
        return TransportEndpointResolver.resolveTls(tlsConfig);
    }
}
