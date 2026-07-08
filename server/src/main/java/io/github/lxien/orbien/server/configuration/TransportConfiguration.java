package io.github.lxien.orbien.server.configuration;

import io.github.lxien.orbien.core.notify.EventBus;
import io.github.lxien.orbien.server.transport.TransportListenerManager;
import io.github.lxien.orbien.server.config.AppConfig;
import io.github.lxien.orbien.server.transport.http.BasicAuthHandler;
import io.github.lxien.orbien.server.transport.http.ForceHttpsRedirectHandler;
import io.github.lxien.orbien.server.transport.http.HttpIpCheckHandler;
import io.github.lxien.orbien.server.transport.http.HttpProxyServer;
import io.github.lxien.orbien.server.transport.http.HttpVisitorHandler;
import io.github.lxien.orbien.server.transport.https.TlsCertificateManager;
import io.github.lxien.orbien.server.transport.https.HttpsProxyServer;
import io.github.lxien.orbien.server.transport.tcp.TcpIpCheckHandler;
import io.github.lxien.orbien.server.transport.tcp.TcpVisitorHandler;
import io.github.lxien.orbien.server.transport.tcp.TcpProxyServer;
import io.github.lxien.orbien.server.transport.udp.UdpProxyServer;
import io.github.lxien.orbien.server.transport.udp.UdpIpCheckHandler;
import io.github.lxien.orbien.server.transport.udp.UdpVisitorHandler;
import io.github.lxien.orbien.server.transport.ControlFrameHandler;
import io.github.lxien.orbien.server.transport.socks5.Socks5HandshakeHandler;
import io.github.lxien.orbien.server.transport.socks5.Socks5IpCheckHandler;
import io.github.lxien.orbien.server.transport.socks5.Socks5ProxyServer;
import io.github.lxien.orbien.server.transport.socks5.Socks5RelayHandler;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import(ManagerConfiguration.class)
public class TransportConfiguration {
    @Resource
    private AppConfig config;

    @Bean
    public TransportListenerManager transportListenerManager(EventBus eventBus, ControlFrameHandler controlFrameHandler) {
        return new TransportListenerManager(config, eventBus, controlFrameHandler);
    }

    @Bean
    public TcpProxyServer tcpProxyServer(TcpVisitorHandler tcpVisitorHandler, TcpIpCheckHandler tcpIpCheckHandler) {
        return new TcpProxyServer(tcpVisitorHandler, tcpIpCheckHandler, config);
    }

    @Bean
    public Socks5ProxyServer socks5ProxyServer(Socks5HandshakeHandler handshakeHandler,
                                               Socks5RelayHandler relayHandler,
                                               Socks5IpCheckHandler ipCheckHandler) {
        return new Socks5ProxyServer(handshakeHandler, relayHandler, ipCheckHandler, config);
    }

    @Bean
    public UdpProxyServer udpProxyServer(UdpVisitorHandler udpVisitorHandler, UdpIpCheckHandler udpIpCheckHandler, EventBus eventBus) {
        return new UdpProxyServer(udpVisitorHandler, udpIpCheckHandler, eventBus);
    }

    @Bean
    public HttpProxyServer httpProxyServer(HttpVisitorHandler httpVisitorHandler,
                                           HttpIpCheckHandler httpIpCheckHandler,
                                           BasicAuthHandler basicAuthHandler,
                                           ForceHttpsRedirectHandler forceHttpsRedirectHandler) {
        return new HttpProxyServer(config,
                httpVisitorHandler,
                httpIpCheckHandler,
                basicAuthHandler,
                forceHttpsRedirectHandler
        );
    }
    @Bean
    public HttpsProxyServer httpsProxyServer(HttpVisitorHandler httpVisitorHandler,
                                             HttpIpCheckHandler httpIpCheckHandler,
                                             BasicAuthHandler basicAuthHandler,
                                             TlsCertificateManager tlsCertificateManager) {
        return new HttpsProxyServer(config,
                httpVisitorHandler,
                httpIpCheckHandler,
                basicAuthHandler,
                tlsCertificateManager);
    }
}
