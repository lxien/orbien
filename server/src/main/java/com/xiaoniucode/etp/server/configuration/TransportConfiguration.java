package com.xiaoniucode.etp.server.configuration;

import com.xiaoniucode.etp.core.notify.EventBus;
import com.xiaoniucode.etp.server.TunnelServer;
import com.xiaoniucode.etp.server.config.AppConfig;
import com.xiaoniucode.etp.server.transport.https.SslCertificateManager;
import com.xiaoniucode.etp.server.transport.http.*;
import com.xiaoniucode.etp.server.transport.https.HttpsProxyServer;
import com.xiaoniucode.etp.server.transport.tcp.TcpProxyServer;
import com.xiaoniucode.etp.server.transport.ControlFrameHandler;
import com.xiaoniucode.etp.server.transport.tcp.TcpIpCheckHandler;
import com.xiaoniucode.etp.server.transport.tcp.TcpVisitorHandler;
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
    public TunnelServer tunnelServer(EventBus eventBus, ControlFrameHandler controlFrameHandler) {
        return new TunnelServer(config, eventBus, controlFrameHandler);
    }

    @Bean
    public TcpProxyServer tcpProxyServer(TcpVisitorHandler tcpVisitorHandler, TcpIpCheckHandler tcpIpCheckHandler, EventBus eventBus) {
        return new TcpProxyServer(tcpVisitorHandler, tcpIpCheckHandler, eventBus);
    }

    @Bean
    public HttpProxyServer httpProxyServer(HttpVisitorHandler httpVisitorHandler,
                                           HttpIpCheckHandler httpIpCheckHandler,
                                           BasicAuthHandler basicAuthHandler,
                                           EventBus eventBus) {
        return new HttpProxyServer(config,
                httpVisitorHandler,
                httpIpCheckHandler,
                basicAuthHandler,
                eventBus);
    }
    @Bean
    public HttpsProxyServer httpsProxyServer(HttpVisitorHandler httpVisitorHandler,
                                             HttpIpCheckHandler httpIpCheckHandler,
                                             BasicAuthHandler basicAuthHandler,
                                             EventBus eventBus, SslCertificateManager sslCertificateManager) {
        return new HttpsProxyServer(config,
                httpVisitorHandler,
                httpIpCheckHandler,
                basicAuthHandler,
                eventBus,
                sslCertificateManager);
    }
}
