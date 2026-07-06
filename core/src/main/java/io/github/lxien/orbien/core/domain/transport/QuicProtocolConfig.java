package io.github.lxien.orbien.core.domain.transport;

import io.github.lxien.orbien.core.enums.TransportProtocol;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class QuicProtocolConfig extends ProtocolListenerConfig {
    private long maxIdleTimeoutMs = 120_000L;
    private long initialMaxData = 1_048_576L;
    private long initialMaxStreamData = 1_048_576L;
    private int initialMaxStreamsBidi = 100;
    private String congestionControl = "bbr";

    public QuicProtocolConfig() {
        setEnabled(false);
        setPort(TransportProtocol.QUIC.getDefaultPort());
    }
}
