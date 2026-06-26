/*
 *    Copyright 2026 xiaoniucode
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.xiaoniucode.etp.autoconfigure;

import com.xiaoniucode.etp.client.TunnelClient;
import com.xiaoniucode.etp.client.config.AppConfig;
import com.xiaoniucode.etp.client.config.DefaultAppConfig;
import com.xiaoniucode.etp.client.config.domain.AuthConfig;
import com.xiaoniucode.etp.client.config.domain.ConnectionConfig;
import com.xiaoniucode.etp.client.config.domain.RetryConfig;
import com.xiaoniucode.etp.client.config.domain.TransportConfig;
import com.xiaoniucode.etp.core.domain.*;
import com.xiaoniucode.etp.core.enums.AccessControl;
import com.xiaoniucode.etp.core.enums.AgentType;
import com.xiaoniucode.etp.core.enums.ProxyStatus;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.StringUtils;

import java.util.Set;
import java.util.stream.Collectors;

public class ClientBootstrap implements DisposableBean {
    private TunnelClient tunnelClient;
    private final PortHolder portHolder;
    private final Environment environment;
    private final EtpClientProperties properties;
    private final ResourceLoader resourceLoader;
    private volatile boolean started = false;

    public ClientBootstrap(Environment environment, EtpClientProperties properties, PortHolder portHolder, ResourceLoader resourceLoader) {
        this.environment = environment;
        this.properties = properties;
        this.portHolder = portHolder;
        this.resourceLoader = resourceLoader;
    }

    @EventListener
    public void onReady(ApplicationReadyEvent event) {
        if (started) {
            return;
        }
        int port = portHolder.get();
        if (port <= 0) {
            throw new IllegalStateException("Cannot determine local server port");
        }
        String appName = environment.getProperty("spring.application.name", "unknown");
        startClient(appName, port);
        started = true;
    }

    private void startClient(String appName, int localPort) {
        ProxyProperties proxy = properties.getProxy();
        AccessControlProperties accessControl = proxy.getAccessControl();
        BandwidthProperties bandwidth = proxy.getBandwidth();
        TransportCustomProperties transport = proxy.getTransport();
        BasicAuthProperties basicAuth = proxy.getBasicAuth();
        TransportProperties.TlsProperties tls = properties.getTransport().getTls();
        AuthProperties auth = properties.getAuth();
        ConnectionProperties.RetryProperties retry = properties.getConnection().getRetry();

        // 创建并配置 ProxyConfig
        ProxyConfig proxyConfig = new ProxyConfig();
        proxyConfig.setName(appName);
        proxyConfig.setForceHttps(proxy.getForceHttps());
        proxyConfig.addTarget(new Target(proxy.getLocalIp(), localPort, 1, appName));
        proxyConfig.setStatus(ProxyStatus.OPEN);
        proxyConfig.setProtocol(proxy.getProtocol());
        proxyConfig.setRemotePort(proxy.getRemotePort());

        // 配置访问控制
        AccessControlConfig accessControlConfig = new AccessControlConfig(
                accessControl.isEnabled(),
                AccessControl.valueOf(accessControl.getMode().name()),
                accessControl.getAllow(),
                accessControl.getDeny()
        );
        proxyConfig.setAccessControl(accessControlConfig);
        if (StringUtils.hasText(bandwidth.getLimitTotal()) ||
                StringUtils.hasText(bandwidth.getLimitIn()) ||
                StringUtils.hasText(bandwidth.getLimitOut())) {
            // 配置带宽限制
            BandwidthConfig bandwidthConfig = new BandwidthConfig(
                    bandwidth.getLimitTotal(),
                    bandwidth.getLimitIn(),
                    bandwidth.getLimitOut()
            );

            proxyConfig.setBandwidth(bandwidthConfig);
        }
        // 配置域名信息
        RouteConfig routeConfig = new RouteConfig();
        routeConfig.setAutoDomain(proxy.getAutoDomain());
        routeConfig.getCustomDomains().addAll(proxy.getCustomDomains());
        routeConfig.getSubDomains().addAll(proxy.getSubDomains());
        proxyConfig.setRouteConfig(routeConfig);

        // 配置基础认证
        if (basicAuth.isEnabled() && !basicAuth.getUsers().isEmpty()) {
            Set<HttpUser> users = basicAuth.getUsers().stream()
                    .map(user -> new HttpUser(user.getUser(), user.getPass()))
                    .collect(Collectors.toSet());

            BasicAuthConfig basicAuthConfig = new BasicAuthConfig();
            basicAuthConfig.setEnabled(basicAuth.isEnabled());
            basicAuthConfig.addUsers(users);

            proxyConfig.setBasicAuth(basicAuthConfig);
        }

        // 配置传输
        TransportCustomConfig transportCustomConfig = new TransportCustomConfig(
                transport.isMultiplex(),
                transport.isEncrypt(),
                false
        );
        proxyConfig.setTransport(transportCustomConfig);

        // 配置TLS
        TlsConfig tlsConfig = new TlsConfig(tls.getEnabled());
        tlsConfig.setCertFile(getAbsolutePath(tls.getCertFile()));
        tlsConfig.setKeyFile(getAbsolutePath(tls.getKeyFile()));
        tlsConfig.setCaFile(getAbsolutePath(tls.getCaFile()));
        tlsConfig.setKeyPassword(tls.getKeyPassword());

        // 配置认证
        AuthConfig authConfig = new AuthConfig();
        authConfig.setToken(auth.getToken());
        RetryConfig retryConfig = new RetryConfig();
        retryConfig.setInitialDelay(retry.getInitialDelay());
        retryConfig.setMaxDelay(retry.getMaxDelay());
        retryConfig.setMaxRetries(retry.getMaxRetries());

        // 配置传输
        TransportConfig transportConfig = new TransportConfig();
        transportConfig.setTlsConfig(tlsConfig);

        // 配置连接
        ConnectionConfig connectionConfig = new ConnectionConfig();
        connectionConfig.setRetryConfig(retryConfig);

        // 构建 AppConfig
        AppConfig config = new DefaultAppConfig
                .Builder()
                .serverAddr(properties.getServerAddr())
                .serverPort(properties.getServerPort())
                .agentType(AgentType.EMBEDDED)
                .transportConfig(transportConfig)
                .connectionConfig(connectionConfig)
                .authConfig(authConfig)
                .addProxy(proxyConfig)
                .build();

        tunnelClient = new TunnelClient(config);
        tunnelClient.start();
        started = true;
    }

    public String getAbsolutePath(String location) {
        try {
            if (!StringUtils.hasText(location)) {
                return null;
            }
            Resource resource = resourceLoader.getResource(location);
            return resource.getFile().getAbsolutePath();
        } catch (Exception e) {
            throw new RuntimeException("无法加载 TLS 证书文件: " + location, e);
        }
    }

    @Override
    public void destroy() {
        if (tunnelClient != null) {
            try {
                tunnelClient.stop();
            } catch (Exception ignored) {
            }
        }
    }
}