package io.github.lxien.orbien.client;

import io.github.lxien.orbien.client.config.AppConfig;
import io.github.lxien.orbien.client.config.ConfigUtils;
import io.github.lxien.orbien.client.identity.AgentIdentity;
import io.github.lxien.orbien.client.transport.ControlFrameHandler;
import io.github.lxien.orbien.client.transport.ControlIdleCheckHandler;
import io.github.lxien.orbien.client.transport.HeartbeatHandler;
import io.github.lxien.orbien.client.transport.RealServerHandler;
import io.github.lxien.orbien.client.statemachine.agent.AgentContext;
import io.github.lxien.orbien.client.statemachine.agent.AgentStateMachineBuilder;
import io.github.lxien.orbien.client.statemachine.agent.AgentEvent;
import io.github.lxien.orbien.client.transport.connection.DirectPool;
import io.github.lxien.orbien.client.transport.connection.MultiplexPool;
import io.github.lxien.orbien.core.codec.TMSPCodec;
import io.github.lxien.orbien.core.transport.IdleCheckHandler;
import io.github.lxien.orbien.client.transport.UdpRealServerHandler;
import io.github.lxien.orbien.core.transport.NettyConstants;
import io.github.lxien.orbien.core.transport.NettyEventLoopFactory;
import io.github.lxien.orbien.core.server.Lifecycle;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.compression.SnappyFrameDecoder;
import io.netty.handler.codec.compression.SnappyFrameEncoder;
import io.netty.handler.ssl.SslHandler;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * 代理客户端服务容器
 *
 * @author liuxin
 */
public final class TunnelClient implements Lifecycle {
    private final InternalLogger logger = InternalLoggerFactory.getInstance(TunnelClient.class);
    private final AppConfig config;
    private EventLoopGroup controlWorkerGroup;
    private EventLoopGroup serverWorkBootstrap;
    private AgentContext agentContext;


    public TunnelClient(AppConfig config) {
        this.config = config;
    }

    @Override
    public void start() {
        try {
            ConfigUtils.setConfig(config);
            initializeStateMachine();
            agentContext.fireEvent(AgentEvent.START);
        } catch (Exception e) {
            logger.error(e);
        }
    }

    private void initializeStateMachine() {
        agentContext = new AgentContext(config, AgentStateMachineBuilder.getStateMachine());
        agentContext.setTunnelClient(this);
        agentContext.setDirectPool(new DirectPool());
        agentContext.setMultiplexPool(new MultiplexPool());
        agentContext.setAgentIdentity(new AgentIdentity());

        serverWorkBootstrap = NettyEventLoopFactory.eventLoopGroup();
        Bootstrap serverBootstrap = new Bootstrap()
                .group(serverWorkBootstrap)
                .channel(NettyEventLoopFactory.socketChannelClass())
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ChannelPipeline p = ch.pipeline();
                        p.addLast(new IdleCheckHandler());
                        p.addLast(NettyConstants.REAL_SERVER_HANDLER, new RealServerHandler());
                    }
                });
        Bootstrap udpServerBootstrap = new Bootstrap()
                .group(serverWorkBootstrap)
                .channel(NettyEventLoopFactory.datagramChannelClass())
                .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .handler(new ChannelInitializer<DatagramChannel>() {
                    @Override
                    protected void initChannel(DatagramChannel ch) {
                        ch.pipeline().addLast(NettyConstants.UDP_REAL_SERVER_HANDLER, new UdpRealServerHandler());
                    }
                });
        Bootstrap controlBootstrap = new Bootstrap();
        ControlFrameHandler controlTunnelHandler = new ControlFrameHandler(agentContext);
        agentContext.setControlFrameHandler(controlTunnelHandler);
        controlWorkerGroup = NettyEventLoopFactory.eventLoopGroup();
        controlBootstrap.group(controlWorkerGroup)
                .channel(NettyEventLoopFactory.socketChannelClass())
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel sc) {
                        if (agentContext.getTlsContext() != null) {
                            SslHandler sslHandler = agentContext.getTlsContext().newHandler(
                                    sc.alloc(), config.getServerAddr(), config.getServerPort());
                            sc.pipeline().addLast(NettyConstants.TLS_HANDLER, sslHandler);
                        }
                        sc.pipeline()
                                .addLast(new SnappyFrameEncoder())
                                .addLast(new SnappyFrameDecoder())
                                .addLast(NettyConstants.TMSP_CODEC, TMSPCodec.create(10 * 1024 * 1024))
                                .addLast(NettyConstants.CONTROL_IDLE_CHECK_HANDLER, new ControlIdleCheckHandler(agentContext, 90, 0, 0, TimeUnit.SECONDS))
                                .addLast(new HeartbeatHandler(30))
                                .addLast(NettyConstants.CONTROL_FRAME_HANDLER, controlTunnelHandler);
                    }
                });
        agentContext.setControlBootstrap(controlBootstrap);
        agentContext.setControlWorkerGroup(controlWorkerGroup);
        agentContext.setServerBootstrap(serverBootstrap);
        agentContext.setUdpServerBootstrap(udpServerBootstrap);
        agentContext.setServerWorkerGroup(serverWorkBootstrap);
    }

    @Override
    public void stop() {
        if (controlWorkerGroup != null) {
            controlWorkerGroup.shutdownGracefully();
        }
        if (serverWorkBootstrap != null) {
            serverWorkBootstrap.shutdownGracefully();
        }
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        //Runtime.getRuntime().halt(0);
    }
}
