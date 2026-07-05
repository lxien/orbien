package io.github.lxien.orbien.core.domain.transport;

import io.github.lxien.orbien.core.domain.TlsConfig;
import lombok.Data;

@Data
public class ProtocolListenerConfig {
    private boolean enabled = true;
    private String addr;
    private int port;
    private TlsConfig tlsConfig = new TlsConfig(true);
}
