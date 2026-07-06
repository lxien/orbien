package io.github.lxien.orbien.core.domain.transport;

import io.github.lxien.orbien.core.enums.TransportProtocol;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class WebSocketProtocolConfig extends ProtocolListenerConfig {
    private String path = "/tunnel";
    private int maxFrameSize = 10 * 1024 * 1024;

    public WebSocketProtocolConfig() {
        setEnabled(false);
        setPort(TransportProtocol.WEBSOCKET.getDefaultPort());
    }
}
