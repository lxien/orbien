package io.github.lxien.orbien.core.domain.transport;

import lombok.Data;

@Data
public class ProtocolListenerConfig {
    private boolean enabled = false;
    private int port;
}
