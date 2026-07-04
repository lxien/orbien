package io.github.lxien.orbien.client.config;

import io.github.lxien.orbien.client.config.domain.*;
import io.github.lxien.orbien.core.domain.ProxyConfig;
import io.github.lxien.orbien.core.enums.AgentType;
import io.github.lxien.orbien.client.config.domain.AuthConfig;
import io.github.lxien.orbien.client.config.domain.ConnectionConfig;
import io.github.lxien.orbien.client.config.domain.LogConfig;
import io.github.lxien.orbien.client.config.domain.TransportConfig;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class DefaultAppConfig implements AppConfig {
    private final String serverAddr;
    private final int serverPort;
    private final AuthConfig authConfig;
    private final List<ProxyConfig> proxies;
    private final LogConfig logConfig;
    private final AgentType agentType;
    private final TransportConfig transportConfig;
    private final ConnectionConfig connectionConfig;

    private DefaultAppConfig(Builder builder) {
        this.serverAddr = builder.serverAddr;
        this.serverPort = builder.serverPort;
        this.authConfig = builder.authConfig;
        this.proxies = builder.proxies;
        this.logConfig = builder.logConfig;
        this.agentType = builder.agentType;
        this.transportConfig = builder.transportConfig;
        this.connectionConfig = builder.connectionConfig;
    }

    public static class Builder {
        private String serverAddr = "127.0.0.1";
        private int serverPort = 9527;
        private AuthConfig authConfig = new AuthConfig();
        private final List<ProxyConfig> proxies = new CopyOnWriteArrayList<>();
        private LogConfig logConfig;
        private AgentType agentType = AgentType.STANDALONE;
        private TransportConfig transportConfig = new TransportConfig();
        private ConnectionConfig connectionConfig = new ConnectionConfig();

        public Builder serverAddr(String serverAddr) {
            this.serverAddr = serverAddr;
            return this;
        }

        public Builder serverPort(int serverPort) {
            this.serverPort = serverPort;
            return this;
        }

        public Builder authConfig(AuthConfig authConfig) {
            this.authConfig = authConfig;
            return this;
        }
        public Builder transportConfig(TransportConfig transportConfig) {
            this.transportConfig = transportConfig;
            return this;
        }

        public Builder connectionConfig(ConnectionConfig connectionConfig) {
            this.connectionConfig = connectionConfig;
            return this;
        }

        public Builder addProxies(List<ProxyConfig> proxies) {
            if (proxies != null) {
                this.proxies.addAll(proxies);
            }
            return this;
        }

        public Builder addProxy(ProxyConfig proxyConfig) {
            this.proxies.add(proxyConfig);
            return this;
        }

        public Builder logConfig(LogConfig logConfig) {
            this.logConfig = logConfig;
            return this;
        }

        public Builder agentType(AgentType agentType) {
            this.agentType = agentType;
            return this;
        }

        public AppConfig build() {
            return new DefaultAppConfig(this);
        }
    }

    @Override
    public String getServerAddr() {
        return serverAddr;
    }

    @Override
    public int getServerPort() {
        return serverPort;
    }

    @Override
    public AuthConfig getAuthConfig() {
        return authConfig;
    }

      @Override
    public List<ProxyConfig> getProxies() {
        return proxies;
    }

    @Override
    public LogConfig getLogConfig() {
        return logConfig;
    }

    @Override
    public AgentType getAgentType() {
        return agentType;
    }

    @Override
    public TransportConfig getTransportConfig() {
        return transportConfig;
    }

    @Override
    public ConnectionConfig getConnectionConfig() {
        return connectionConfig;
    }

    public static Builder builder() {
        return new Builder();
    }
}
