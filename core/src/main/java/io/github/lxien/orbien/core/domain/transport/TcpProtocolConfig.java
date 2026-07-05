package io.github.lxien.orbien.core.domain.transport;

import io.github.lxien.orbien.core.enums.TransportProtocol;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class TcpProtocolConfig extends ProtocolListenerConfig {
    public TcpProtocolConfig() {
        setPort(TransportProtocol.TCP.getDefaultPort());
    }
}
