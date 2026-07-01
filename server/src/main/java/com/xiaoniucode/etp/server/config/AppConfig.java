package com.xiaoniucode.etp.server.config;

import com.xiaoniucode.etp.common.config.Config;
import com.xiaoniucode.etp.server.config.domain.*;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
public class AppConfig implements Config {
    private String serverAddr;
    private int serverPort;
    private int httpProxyPort;
    private int httpsProxyPort;
    private DashboardConfig dashboard;
    private PortPolicyConfig portPolicy;
    private Set<String> baseDomains;
    private TransportConfig transportConfig;
    private AuthConfig authConfig;

    private AppConfig(Builder builder) {
        this.serverAddr = builder.serverAddr;
        this.serverPort = builder.serverPort;
        this.httpProxyPort = builder.httpProxyPort;
        this.httpsProxyPort = builder.httpsProxyPort;
        this.dashboard = builder.dashboard;
        this.portPolicy = builder.portPolicy;
        this.baseDomains = builder.baseDomains;
        this.transportConfig = builder.transportConfig;
        this.authConfig = builder.authConfig;
    }

    public static class Builder {
        private String serverAddr = "0.0.0.0";
        private int serverPort = 9527;
        private int httpProxyPort = 80;
        private int httpsProxyPort = 443;
        private TransportConfig transportConfig = new TransportConfig();
        private Set<String> baseDomains;
        private DashboardConfig dashboard = new DashboardConfig(false);
        private PortPolicyConfig portPolicy = new PortPolicyConfig(1, 65535);
        private AuthConfig authConfig = new AuthConfig();

        public Builder serverAddr(String serverAddr) {
            this.serverAddr = serverAddr;
            return this;
        }

        public Builder serverPort(int serverPort) {
            this.serverPort = serverPort;
            return this;
        }

        public Builder transport(TransportConfig transportConfig) {
            this.transportConfig = transportConfig;
            return this;
        }

        public Builder dashboard(DashboardConfig dashboard) {
            this.dashboard = dashboard;
            return this;
        }

        public Builder portPolicy(PortPolicyConfig portPolicy) {
            this.portPolicy = portPolicy;
            return this;
        }

        public Builder authConfig(AuthConfig authConfig) {
            this.authConfig = authConfig;
            return this;
        }

        public Builder httpProxyPort(int httpProxyPort) {
            this.httpProxyPort = httpProxyPort;
            return this;
        }

        public Builder httpsProxyPort(int httpsProxyPort) {
            this.httpsProxyPort = httpsProxyPort;
            return this;
        }

        public Builder baseDomain(Set<String> baseDomains) {
            this.baseDomains = baseDomains;
            return this;
        }

        public AppConfig build() {
            return new AppConfig(this);
        }
    }


    public static Builder builder() {
        return new Builder();
    }
}
