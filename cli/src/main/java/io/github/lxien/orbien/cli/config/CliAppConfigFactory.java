package io.github.lxien.orbien.cli.config;

import io.github.lxien.orbien.client.config.AppConfig;
import io.github.lxien.orbien.client.config.DefaultAppConfig;
import io.github.lxien.orbien.client.config.domain.AuthConfig;
import io.github.lxien.orbien.client.config.domain.MultiplexConfig;
import io.github.lxien.orbien.client.config.domain.TransportConfig;
import io.github.lxien.orbien.core.domain.ProxyConfig;
import io.github.lxien.orbien.core.domain.RouteConfig;
import io.github.lxien.orbien.core.domain.Target;
import io.github.lxien.orbien.core.domain.TransportCustomConfig;
import io.github.lxien.orbien.core.domain.TlsConfig;
import io.github.lxien.orbien.core.enums.AgentType;
import io.github.lxien.orbien.core.enums.ProtocolType;
import io.github.lxien.orbien.core.enums.ProxyStatus;
import io.github.lxien.orbien.core.enums.TransportProtocol;
import io.github.lxien.orbien.core.utils.StringUtils;
import io.github.lxien.orbien.cli.credentials.Credentials;

public final class CliAppConfigFactory {

    private CliAppConfigFactory() {
    }

    public static AppConfig buildHttp(Credentials credentials, String host, int port, String domain) {
        ProxyConfig proxy = baseProxy("cli-http-" + port, ProtocolType.HTTP, host, port, null);
        RouteConfig routeConfig = new RouteConfig();
        if (StringUtils.hasText(domain)) {
            routeConfig.setAutoDomain(false);
            routeConfig.addSubDomain(domain.trim());
        } else {
            routeConfig.setAutoDomain(true);
        }
        proxy.setRouteConfig(routeConfig);
        return build(credentials, proxy);
    }

    public static AppConfig buildTcp(Credentials credentials, String host, int port, Integer remotePort) {
        ProxyConfig proxy = baseProxy("cli-tcp-" + port, ProtocolType.TCP, host, port, remotePort);
        return build(credentials, proxy);
    }

    public static AppConfig buildUdp(Credentials credentials, String host, int port, Integer remotePort) {
        ProxyConfig proxy = baseProxy("cli-udp-" + port, ProtocolType.UDP, host, port, remotePort);
        TransportCustomConfig transport = proxy.getTransport();
        transport.setMultiplex(true);
        proxy.setTransport(transport);
        return build(credentials, proxy);
    }

    private static AppConfig build(Credentials credentials, ProxyConfig proxy) {
        AuthConfig authConfig = new AuthConfig();
        authConfig.setToken(credentials.getToken());

        TransportConfig transportConfig = new TransportConfig();
        transportConfig.setProtocol(TransportProtocol.TCP);
        transportConfig.setMultiplexConfig(new MultiplexConfig(true));
        transportConfig.setTlsConfig(new TlsConfig(true));

        return DefaultAppConfig.builder()
                .serverAddr(credentials.getServerAddr())
                .serverPort(credentials.getServerPort())
                .authConfig(authConfig)
                .transportConfig(transportConfig)
                .agentType(AgentType.SESSION)
                .addProxy(proxy)
                .build();
    }

    private static ProxyConfig baseProxy(String name,
                                         ProtocolType protocol,
                                         String host,
                                         int port,
                                         Integer remotePort) {
        ProxyConfig proxy = new ProxyConfig();
        proxy.setName(name);
        proxy.setProtocol(protocol);
        proxy.setStatus(ProxyStatus.OPEN);
        proxy.addTarget(new Target(host, port, 1, "local"));
        if (remotePort != null) {
            proxy.setRemotePort(remotePort);
        }

        TransportCustomConfig transport = new TransportCustomConfig();
        transport.setMultiplex(true);
        transport.setEncrypt(true);
        transport.setCompress(false);
        if (protocol.isUdp()) {
            transport.setMultiplex(true);
        }
        proxy.setTransport(transport);
        return proxy;
    }
}
