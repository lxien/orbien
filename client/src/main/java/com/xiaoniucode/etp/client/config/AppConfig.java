package com.xiaoniucode.etp.client.config;

import com.xiaoniucode.etp.client.config.domain.*;
import com.xiaoniucode.etp.common.config.Config;
import com.xiaoniucode.etp.core.domain.ProxyConfig;
import com.xiaoniucode.etp.core.enums.AgentType;

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
