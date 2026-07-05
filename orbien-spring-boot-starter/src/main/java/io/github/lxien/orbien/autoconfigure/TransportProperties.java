package io.github.lxien.orbien.autoconfigure;

import io.github.lxien.orbien.core.enums.TransportProtocol;
import lombok.Data;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

import java.io.Serializable;

@Data
public class TransportProperties implements Serializable {
    /**
     * 控制连接与数据隧道 传输协议。
     */
    private TransportProtocol protocol = TransportProtocol.TCP;

    @NestedConfigurationProperty
    private MultiplexProperties multiplex = new MultiplexProperties();

    @NestedConfigurationProperty
    private TlsProperties tls = new TlsProperties();

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
}
