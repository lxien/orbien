package io.github.lxien.orbien.core.domain.transport;

import lombok.Data;

@Data
public class ProtocolListenerConfig {
    private boolean enabled = true;
    private String addr;
    private int port;
}
