package io.github.lxien.orbien.autoconfigure;

import io.github.lxien.orbien.core.enums.TransportProtocol;
import lombok.Data;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

import java.io.Serializable;

@Data
public class TransportProperties implements Serializable {

    private TransportProtocol protocol = TransportProtocol.TCP;

    @NestedConfigurationProperty
    private MultiplexProperties multiplex = new MultiplexProperties();

    @NestedConfigurationProperty
    private TlsProperties tls = new TlsProperties();

    @NestedConfigurationProperty
    private WebSocketProperties websocket = new WebSocketProperties();

    @NestedConfigurationProperty
    private QuicProperties quic = new QuicProperties();

    @Data
    static class MultiplexProperties implements Serializable {
        private boolean enabled = true;
    }

    @Data
    static class TlsProperties implements Serializable {
        private Boolean enabled = true;
        private String certFile;
        private String keyFile;
        private String caFile;
        private String keyPassword;
    }

    @Data
    static class WebSocketProperties implements Serializable {
        private Integer serverPort = 9528;
        private String path = "/tunnel";
    }

    @Data
    static class QuicProperties implements Serializable {
        private Integer serverPort = 9529;
    }
}
