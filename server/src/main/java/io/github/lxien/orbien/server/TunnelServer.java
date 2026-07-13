package io.github.lxien.orbien.server;

import io.github.lxien.orbien.core.codec.TMSPCodec;
import io.github.lxien.orbien.core.domain.TlsConfig;
import io.github.lxien.orbien.core.transport.NettyConstants;
import io.github.lxien.orbien.core.transport.NettyEventLoopFactory;
import io.github.lxien.orbien.core.server.Lifecycle;

import io.github.lxien.orbien.server.notify.EventBus;
import io.github.lxien.orbien.core.transport.tls.TlsHelper;
import io.github.lxien.orbien.server.config.AppConfig;
import io.github.lxien.orbien.server.config.domain.TransportConfig;
import io.github.lxien.orbien.server.configuration.SpringContextHolder;
import io.github.lxien.orbien.server.event.TunnelServerBindEvent;
import io.github.lxien.orbien.server.statemachine.agent.AgentManager;
import io.github.lxien.orbien.server.transport.ControlFrameHandler;
import io.github.lxien.orbien.core.transport.TlsContextHolder;
import io.github.lxien.orbien.server.transport.ControlIdleCheckHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.ssl.OptionalSslHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import java.util.concurrent.TimeUnit;

/**
 * 控制隧道服务容器
 *
 * @author lxien
 */
public class TunnelServer implements Lifecycle {
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(TunnelServer.class);
    private final AppConfig config;
    private EventLoopGroup tunnelBossGroup;
    private EventLoopGroup tunnelWorkerGroup;
    private SslContext tlsContext;
    private final EventBus eventBus;
    private final ControlFrameHandler controlFrameHandler;

    public TunnelServer(AppConfig config, EventBus eventBus, ControlFrameHandler controlFrameHandler) {
        this.config = config;
        this.eventBus = eventBus;
        this.controlFrameHandler = controlFrameHandler;
    }

    @SuppressWarnings("all")
    @Override
    @PostConstruct
    public void start() {
        try {
            logger.debug("正在启动Orbien服务");
            TransportConfig transportConfig = config.getTransportConfig();
            TlsConfig tlsConfig = transportConfig.getTlsConfig();

            if (tlsConfig == null || tlsConfig.isEnabled()) {
                tlsContext = TlsHelper.buildSslContext(false, tlsConfig, tlsConfig == null);
                TlsContextHolder.initialize(tlsContext);
            }
            tunnelBossGroup = NettyEventLoopFactory.eventLoopGroup(1);
            tunnelWorkerGroup = NettyEventLoopFactory.eventLoopGroup();
            AgentManager agentManager = SpringContextHolder.getBean(AgentManager.class);
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            serverBootstrap.group(tunnelBossGroup, tunnelWorkerGroup)
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    .childOption(ChannelOption.TCP_NODELAY, true)
                    .childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                    .channel(NettyEventLoopFactory.serverSocketChannelClass())
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel sc) {
                            if (tlsContext != null) {
                                sc.pipeline().addLast(new OptionalSslHandler(tlsContext));
                            }
                            sc.pipeline()
                                    .addLast(NettyConstants.TMSP_CODEC, TMSPCodec.create(10 * 1024 * 1024))
                                    .addLast(NettyConstants.CONTROL_IDLE_CHECK_HANDLER, new ControlIdleCheckHandler(agentManager, 90, 0, 0, TimeUnit.SECONDS))
                                    .addLast(NettyConstants.CONTROL_FRAME_HANDLER, controlFrameHandler);
                        }
                    });
            serverBootstrap.bind(config.getServerAddr(), config.getServerPort()).sync();
            logger.info("Orbien隧道已开启监听:{}:{}", config.getServerAddr(), config.getServerPort());
            eventBus.publishAsync(new TunnelServerBindEvent());
        } catch (Throwable e) {
            logger.error("orbien隧道开启失败", e);
        }
    }

    @Override
    @PreDestroy
    public void stop() {
        try {
            tunnelBossGroup.shutdownGracefully().sync();
            tunnelWorkerGroup.shutdownGracefully().sync();
        } catch (InterruptedException e) {
            logger.error(e.getMessage());
        }
    }
}
