package io.github.lxien.orbien.server.transport;
import io.github.lxien.orbien.core.enums.TransportProtocol;

import io.github.lxien.orbien.core.domain.TlsConfig;
import io.github.lxien.orbien.core.domain.transport.QuicProtocolConfig;
import io.github.lxien.orbien.core.domain.transport.TcpProtocolConfig;
import io.github.lxien.orbien.core.domain.transport.WebSocketProtocolConfig;
import io.github.lxien.orbien.core.notify.EventBus;
import io.github.lxien.orbien.core.server.Lifecycle;
import io.github.lxien.orbien.core.transport.NettyConstants;
import io.github.lxien.orbien.core.transport.NettyEventLoopFactory;
import io.github.lxien.orbien.core.transport.api.*;
import io.github.lxien.orbien.core.transport.tls.TlsHelper;
import io.github.lxien.orbien.core.transport.TlsContextHolder;
import io.github.lxien.orbien.server.config.AppConfig;
import io.github.lxien.orbien.server.config.domain.TransportConfig;
import io.github.lxien.orbien.server.event.TunnelServerBindEvent;
import io.github.lxien.orbien.server.configuration.SpringContextHolder;
import io.github.lxien.orbien.server.statemachine.agent.AgentManager;
import io.netty.channel.EventLoopGroup;
import io.netty.handler.ssl.SslContext;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class TransportListenerManager implements Lifecycle {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(TransportListenerManager.class);

    private final AppConfig config;
    private final EventBus eventBus;
    private final ControlFrameHandler controlFrameHandler;
    private final List<TransportListener> startedListeners = new ArrayList<>();
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;

    public TransportListenerManager(AppConfig config, EventBus eventBus, ControlFrameHandler controlFrameHandler) {
        this.config = config;
        this.eventBus = eventBus;
        this.controlFrameHandler = controlFrameHandler;
    }

    @Override
    @PostConstruct
    public void start() {
        try {
            logger.debug("正在启动 Orbien 传输监听");
            TransportConfig transportConfig = config.getTransportConfig();
            TlsConfig sharedTls = transportConfig.getTlsConfig();

            SslContext tlsContext = buildSharedTlsContext(sharedTls);
            if (tlsContext != null) {
                TlsContextHolder.initialize(tlsContext);
            }

            bossGroup = NettyEventLoopFactory.eventLoopGroup(1);
            workerGroup = NettyEventLoopFactory.eventLoopGroup();

            MultiplexDownloadRateLimitHandler downloadRateLimitHandler =
                    SpringContextHolder.getBean(MultiplexDownloadRateLimitHandler.class);
            AgentManager agentManager = SpringContextHolder.getBean(AgentManager.class);

            TcpProtocolConfig tcp = transportConfig.getTcp();
            if (tcp.isEnabled()) {
                startListener(TransportProtocol.TCP, null, config.getServerPort(), tlsContext, sharedTls,
                        downloadRateLimitHandler, agentManager, null, transportConfig.getQuic());
            }

            WebSocketProtocolConfig websocket = transportConfig.getWebsocket();
            if (websocket.isEnabled()) {
                startListener(TransportProtocol.WEBSOCKET, websocket.getAddr(), websocket.getPort(), tlsContext, sharedTls,
                        downloadRateLimitHandler, agentManager, websocket, transportConfig.getQuic());
            }

            QuicProtocolConfig quic = transportConfig.getQuic();
            if (quic.isEnabled()) {
                startListener(TransportProtocol.QUIC, quic.getAddr(), quic.getPort(), tlsContext, sharedTls,
                        downloadRateLimitHandler, agentManager, transportConfig.getWebsocket(), quic);
            }

            eventBus.publishAsync(new TunnelServerBindEvent());
        } catch (Throwable e) {
            logger.error("Orbien 传输监听启动失败", e);
        }
    }

    private void startListener(TransportProtocol protocol,
                               String addr,
                               int port,
                               SslContext tlsContext,
                               TlsConfig tlsConfig,
                               MultiplexDownloadRateLimitHandler downloadRateLimitHandler,
                               AgentManager agentManager,
                               WebSocketProtocolConfig webSocketConfig,
                               QuicProtocolConfig quicConfig) {
        if (addr == null || addr.isBlank()) {
            addr = config.getServerAddr();
        }
        TransportListener listener = TransportRegistry.getListener(protocol);
        TransportBindOptions bindOptions = TransportBindOptions.builder()
                .protocol(protocol)
                .addr(addr)
                .port(port)
                .sslContext(tlsContext)
                .tlsConfig(tlsConfig)
                .webSocketConfig(webSocketConfig)
                .quicConfig(quicConfig)
                .bossGroup(bossGroup)
                .workerGroup(workerGroup)
                .pipelineConfigurer((channel, pipeline) -> pipeline
                      //  .addLast(downloadRateLimitHandler)
                        .addLast(NettyConstants.CONTROL_IDLE_CHECK_HANDLER,
                                new ControlIdleCheckHandler(agentManager, 90, 0, 0, TimeUnit.SECONDS))
                        .addLast(NettyConstants.CONTROL_FRAME_HANDLER, controlFrameHandler))
                .build();
        listener.bind(bindOptions, session -> {
        });
        listener.start();
        startedListeners.add(listener);
        logger.info("[传输] 监听已启动 protocol={} addr={}:{}", protocol.getName(), addr, port);
    }

    private SslContext buildSharedTlsContext(TlsConfig tlsConfig) throws Exception {
        if (tlsConfig != null && tlsConfig.isEnabled()) {
            return TlsHelper.buildSslContext(false, tlsConfig, false);
        }
        return TlsHelper.buildSslContext(false, tlsConfig, true);
    }

    @Override
    @PreDestroy
    public void stop() {
        for (TransportListener listener : startedListeners) {
            listener.stop();
        }
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }
    }
}
