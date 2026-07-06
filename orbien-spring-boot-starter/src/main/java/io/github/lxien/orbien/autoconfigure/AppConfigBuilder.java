package io.github.lxien.orbien.autoconfigure;

import io.github.lxien.orbien.client.config.AppConfig;
import io.github.lxien.orbien.client.config.DefaultAppConfig;
import io.github.lxien.orbien.client.config.domain.AuthConfig;
import io.github.lxien.orbien.client.config.domain.ConnectionConfig;
import io.github.lxien.orbien.client.config.domain.MultiplexConfig;
import io.github.lxien.orbien.client.config.domain.PoolConfig;
import io.github.lxien.orbien.client.config.domain.RetryConfig;
import io.github.lxien.orbien.client.config.domain.TransportConfig;
import io.github.lxien.orbien.core.domain.AccessControlConfig;
import io.github.lxien.orbien.core.domain.BasicAuthConfig;
import io.github.lxien.orbien.core.domain.BandwidthConfig;
import io.github.lxien.orbien.core.domain.HttpUser;
import io.github.lxien.orbien.core.domain.ProxyConfig;
import io.github.lxien.orbien.core.domain.RouteConfig;
import io.github.lxien.orbien.core.domain.Target;
import io.github.lxien.orbien.core.domain.TlsConfig;
import io.github.lxien.orbien.core.domain.TransportCustomConfig;
import io.github.lxien.orbien.core.enums.AccessControl;
import io.github.lxien.orbien.core.enums.AgentType;
import io.github.lxien.orbien.core.enums.ProxyStatus;
import io.github.lxien.orbien.core.enums.TransportProtocol;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.StringUtils;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * 将 Spring Boot 配置属性组装为客户端 {@link AppConfig}。
 * <p>
 * 本地 proxy 配置仅用于启动时向服务端上报
 */
final class AppConfigBuilder {

    private AppConfigBuilder() {
    }

    static AppConfig build(OrbienClientProperties properties,
                           ResourceLoader resourceLoader,
                           String appName,
                           int localPort) {
        return DefaultAppConfig.builder()
                .serverAddr(properties.getServerAddr())
                .serverPort(properties.getServerPort())
                .agentType(AgentType.EMBEDDED)
                .authConfig(buildAuthConfig(properties))
                .transportConfig(buildTransportConfig(properties, resourceLoader))
                .connectionConfig(buildConnectionConfig(properties))
                .addProxy(buildProxyConfig(properties, appName, localPort))
                .build();
    }

    private static AuthConfig buildAuthConfig(OrbienClientProperties properties) {
        AuthConfig authConfig = new AuthConfig();
        authConfig.setToken(properties.getAuth().getToken());
        return authConfig;
    }

    private static TransportConfig buildTransportConfig(OrbienClientProperties properties,
                                                         ResourceLoader resourceLoader) {
        TransportProperties transportProps = properties.getTransport();
        TransportProperties.TlsProperties tlsProps = transportProps.getTls();

        TransportConfig transportConfig = new TransportConfig();
        TransportProtocol controlProtocol = transportProps.getProtocol() != null
                ? transportProps.getProtocol()
                : TransportProtocol.TCP;
        transportConfig.setProtocol(controlProtocol);

        MultiplexConfig multiplexConfig = new MultiplexConfig(
                transportProps.getMultiplex().isEnabled());
        transportConfig.setMultiplexConfig(multiplexConfig);

        TlsConfig tlsConfig = buildTlsConfig(tlsProps, resourceLoader);
        transportConfig.setTlsConfig(tlsConfig);

        if (transportProps.getWebsocket().getServerPort() != null) {
            transportConfig.getWebsocket().setPort(transportProps.getWebsocket().getServerPort());
        }
        if (StringUtils.hasText(transportProps.getWebsocket().getPath())) {
            transportConfig.getWebsocket().setPath(transportProps.getWebsocket().getPath());
        }
        if (transportProps.getQuic().getServerPort() != null) {
            transportConfig.getQuic().setPort(transportProps.getQuic().getServerPort());
        }
        return transportConfig;
    }

    private static TlsConfig buildTlsConfig(TransportProperties.TlsProperties tlsProps,
                                            ResourceLoader resourceLoader) {
        boolean enabled = tlsProps.getEnabled() == null || tlsProps.getEnabled();
        TlsConfig tlsConfig = new TlsConfig(enabled);
        tlsConfig.setCertFile(resolvePath(tlsProps.getCertFile(), resourceLoader));
        tlsConfig.setKeyFile(resolvePath(tlsProps.getKeyFile(), resourceLoader));
        tlsConfig.setCaFile(resolvePath(tlsProps.getCaFile(), resourceLoader));
        tlsConfig.setKeyPassword(tlsProps.getKeyPassword());
        return tlsConfig;
    }

    private static ConnectionConfig buildConnectionConfig(OrbienClientProperties properties) {
        ConnectionProperties connectionProps = properties.getConnection();
        ConnectionConfig connectionConfig = new ConnectionConfig();

        ConnectionProperties.RetryProperties retryProps = connectionProps.getRetry();
        RetryConfig retryConfig = new RetryConfig();
        if (retryProps.getInitialDelay() != null) {
            retryConfig.setInitialDelay(retryProps.getInitialDelay());
        }
        if (retryProps.getMaxDelay() != null) {
            retryConfig.setMaxDelay(retryProps.getMaxDelay());
        }
        if (retryProps.getMaxRetries() != null) {
            retryConfig.setMaxRetries(retryProps.getMaxRetries());
        }
        connectionConfig.setRetryConfig(retryConfig);

        ConnectionProperties.PoolProperties poolProps = connectionProps.getPool();
        PoolConfig poolConfig = new PoolConfig();
        poolConfig.setEnabled(poolProps.isEnabled());
        poolConfig.getMultiplex().setPlain(poolProps.getMultiplex().isPlain());
        poolConfig.getMultiplex().setEncrypt(poolProps.getMultiplex().isEncrypt());
        poolConfig.getDirect().setPlainCount(poolProps.getDirect().getPlainCount());
        poolConfig.getDirect().setEncryptCount(poolProps.getDirect().getEncryptCount());
        connectionConfig.setPoolConfig(poolConfig);

        return connectionConfig;
    }

    private static ProxyConfig buildProxyConfig(OrbienClientProperties properties,
                                                String appName,
                                                int localPort) {
        ProxyProperties proxy = properties.getProxy();

        ProxyConfig proxyConfig = new ProxyConfig();
        proxyConfig.setName(appName);
        proxyConfig.setProtocol(proxy.getProtocol());
        proxyConfig.setRemotePort(proxy.getRemotePort());
        proxyConfig.setStatus(ProxyStatus.OPEN);
        proxyConfig.addTarget(new Target(proxy.getLocalIp(), localPort, 1, appName));

        if (proxy.getProtocol() != null && proxy.getProtocol().isHttps()) {
            Boolean forceHttps = proxy.getForceHttps();
            proxyConfig.setForceHttps(forceHttps == null || forceHttps);
        } else {
            proxyConfig.setForceHttps(false);
        }

        AccessControlProperties accessControl = proxy.getAccessControl();
        AccessControl mode = accessControl.getMode() != null
                ? accessControl.getMode()
                : AccessControl.ALLOW;
        proxyConfig.setAccessControl(new AccessControlConfig(
                accessControl.isEnabled(),
                mode,
                accessControl.getAllow(),
                accessControl.getDeny()
        ));

        BandwidthProperties bandwidth = proxy.getBandwidth();
        if (StringUtils.hasText(bandwidth.getLimitTotal())
                || StringUtils.hasText(bandwidth.getLimitIn())
                || StringUtils.hasText(bandwidth.getLimitOut())) {
            proxyConfig.setBandwidth(new BandwidthConfig(
                    bandwidth.getLimitTotal(),
                    bandwidth.getLimitIn(),
                    bandwidth.getLimitOut()
            ));
        }

        RouteConfig routeConfig = new RouteConfig();
        routeConfig.setAutoDomain(proxy.getAutoDomain());
        routeConfig.getCustomDomains().addAll(proxy.getCustomDomains());
        routeConfig.getSubDomains().addAll(proxy.getSubDomains());
        proxyConfig.setRouteConfig(routeConfig);

        BasicAuthProperties basicAuth = proxy.getBasicAuth();
        if (basicAuth.isEnabled() && !basicAuth.getUsers().isEmpty()) {
            Set<HttpUser> users = basicAuth.getUsers().stream()
                    .map(user -> new HttpUser(user.getUser(), user.getPass()))
                    .collect(Collectors.toSet());
            BasicAuthConfig basicAuthConfig = new BasicAuthConfig();
            basicAuthConfig.setEnabled(true);
            basicAuthConfig.addUsers(users);
            proxyConfig.setBasicAuth(basicAuthConfig);
        }

        TransportCustomProperties transport = proxy.getTransport();
        TransportCustomConfig.TransportCustomConfigBuilder transportBuilder = TransportCustomConfig.builder()
                .multiplex(transport.isMultiplex())
                .encrypt(transport.isEncrypt())
                .compress(transport.isCompress());
        if (transport.getProtocol() != null) {
            transportBuilder.protocol(transport.getProtocol());
        }
        proxyConfig.setTransport(transportBuilder.build());

        return proxyConfig;
    }

    private static String resolvePath(String location, ResourceLoader resourceLoader) {
        if (!StringUtils.hasText(location)) {
            return null;
        }
        try {
            Resource resource = resourceLoader.getResource(location);
            if (resource.isFile()) {
                return resource.getFile().getAbsolutePath();
            }
            return location;
        } catch (Exception e) {
            throw new IllegalStateException("无法加载 TLS 证书文件: " + location, e);
        }
    }
}
