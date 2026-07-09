package io.github.lxien.orbien.client.transport;

import io.github.lxien.orbien.core.enums.TransportProtocol;

import io.github.lxien.orbien.client.config.AppConfig;
import io.github.lxien.orbien.client.config.domain.TransportConfig;
import io.github.lxien.orbien.core.domain.TlsConfig;
import io.github.lxien.orbien.core.transport.api.*;
import io.netty.channel.EventLoopGroup;
import io.netty.handler.ssl.SslContext;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.util.concurrent.CompletableFuture;

public final class TransportClientBootstrap {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(TransportClientBootstrap.class);

    private TransportClientBootstrap() {
    }

    public static CompletableFuture<TransportSession> connectControl(AppConfig config,
                                                                       EventLoopGroup eventLoopGroup,
                                                                       SslContext sslContext,
                                                                       java.util.function.Consumer<io.netty.channel.ChannelPipeline> pipelineTail) {
        TransportConfig transportConfig = config.getTransportConfig();
        TransportProtocol protocol = transportConfig.getProtocol();
        TransportEndpoint endpoint = transportConfig.resolveControlEndpoint(config.getServerAddr(), config.getServerPort());
        return connectControlTunnel(config, protocol, endpoint, eventLoopGroup, sslContext, pipelineTail);
    }

    public static CompletableFuture<TransportSession> connectTunnel(AppConfig config,
                                                                    TransportProtocol protocol,
                                                                    boolean encrypt,
                                                                    EventLoopGroup eventLoopGroup,
                                                                    SslContext sslContext,
                                                                    java.util.function.Consumer<io.netty.channel.ChannelPipeline> pipelineTail) {
        TransportConfig transportConfig = config.getTransportConfig();
        TransportEndpoint endpoint = transportConfig.resolveDataEndpoint(config.getServerAddr(), config.getServerPort(), protocol);
        TlsConfig globalTls = transportConfig.getTlsConfig();
        boolean globalTlsEnabled = globalTls != null && globalTls.isEnabled();
        boolean connectionEncrypt = TransportEncryptResolver.resolveEffectiveEncrypt(protocol, globalTlsEnabled, encrypt);
        TlsConfig tlsConfig = TransportTlsConfigFactory.forDataTunnel(globalTls, protocol, connectionEncrypt);
        return connectTunnel(config, protocol, endpoint, PipelineRole.TUNNEL, eventLoopGroup, sslContext,
                tlsConfig, connectionEncrypt, pipelineTail);
    }

    private static CompletableFuture<TransportSession> connectControlTunnel(AppConfig config,
                                                                            TransportProtocol protocol,
                                                                            TransportEndpoint endpoint,
                                                                            EventLoopGroup eventLoopGroup,
                                                                            SslContext sslContext,
                                                                            java.util.function.Consumer<io.netty.channel.ChannelPipeline> pipelineTail) {
        TransportConfig transportConfig = config.getTransportConfig();
        TlsConfig globalTls = transportConfig.getTlsConfig();
        TlsConfig tlsConfig = TransportTlsConfigFactory.forControlChannel(globalTls);
        boolean connectionEncrypt = tlsConfig.isEnabled() && sslContext != null;
        return connectTunnel(config, protocol, endpoint, PipelineRole.CONTROL, eventLoopGroup, sslContext,
                tlsConfig, connectionEncrypt, pipelineTail);
    }

    private static CompletableFuture<TransportSession> connectTunnel(AppConfig config,
                                                                     TransportProtocol protocol,
                                                                     TransportEndpoint endpoint,
                                                                     PipelineRole role,
                                                                     EventLoopGroup eventLoopGroup,
                                                                     SslContext sslContext,
                                                                     TlsConfig tlsConfig,
                                                                     boolean connectionEncrypt,
                                                                     java.util.function.Consumer<io.netty.channel.ChannelPipeline> pipelineTail) {
        TransportConfig transportConfig = config.getTransportConfig();
        TransportConnectOptions options = TransportConnectOptions.builder()
                .endpoint(endpoint)
                .role(role)
                .eventLoopGroup(eventLoopGroup)
                .sslContext(sslContext)
                .webSocketConfig(transportConfig.getWebsocket())
                .quicConfig(transportConfig.getQuic())
                .tlsConfig(tlsConfig)
                .connectionEncrypt(connectionEncrypt)
                .pipelineTailConfigurer(pipelineTail)
                .build();
        logger.debug("[传输] 发起连接 role={} protocol={} endpoint={}:{} connectionEncrypt={}",
                role, protocol.getName(), endpoint.getHost(), endpoint.getPort(), connectionEncrypt);
        return TransportRegistry.getConnector(protocol).connect(options);
    }
}
