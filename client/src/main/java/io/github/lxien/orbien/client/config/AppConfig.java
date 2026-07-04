package io.github.lxien.orbien.client.config;

import io.github.lxien.orbien.client.config.domain.*;
import io.github.lxien.orbien.common.config.Config;
import io.github.lxien.orbien.core.domain.ProxyConfig;
import io.github.lxien.orbien.core.enums.AgentType;
import io.github.lxien.orbien.client.config.domain.AuthConfig;
import io.github.lxien.orbien.client.config.domain.ConnectionConfig;
import io.github.lxien.orbien.client.config.domain.LogConfig;
import io.github.lxien.orbien.client.config.domain.TransportConfig;

import java.util.List;

public interface AppConfig extends Config {
    String getServerAddr();

    int getServerPort();

    AuthConfig getAuthConfig();

    TransportConfig getTransportConfig();

    ConnectionConfig getConnectionConfig();

    List<ProxyConfig> getProxies();

    LogConfig getLogConfig();

    AgentType getAgentType();

}
