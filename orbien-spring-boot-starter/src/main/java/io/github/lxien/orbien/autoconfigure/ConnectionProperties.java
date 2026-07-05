package io.github.lxien.orbien.autoconfigure;

import lombok.Data;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

import java.io.Serializable;

@Data
public class ConnectionProperties implements Serializable {

    @NestedConfigurationProperty
    private RetryProperties retry = new RetryProperties();

    @NestedConfigurationProperty
    private PoolProperties pool = new PoolProperties();

    @Data
    static class RetryProperties implements Serializable {
        private Integer initialDelay = 1;
        private Integer maxDelay = 20;
        private Integer maxRetries = 5;
    }

    @Data
    static class PoolProperties implements Serializable {
        private boolean enabled = false;

        @NestedConfigurationProperty
        private MultiplexPoolProperties multiplex = new MultiplexPoolProperties();

        @NestedConfigurationProperty
        private DirectPoolProperties direct = new DirectPoolProperties();
    }

    @Data
    static class MultiplexPoolProperties implements Serializable {
        private boolean plain;
        private boolean encrypt;
    }

    @Data
    static class DirectPoolProperties implements Serializable {
        private int plainCount;
        private int encryptCount;
    }
}
