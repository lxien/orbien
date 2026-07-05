package io.github.lxien.orbien.client;

import io.github.lxien.orbien.client.config.AppConfig;
import io.github.lxien.orbien.client.config.ConfigUtils;
import io.github.lxien.orbien.client.identity.AgentIdentity;
import io.github.lxien.orbien.client.transport.ControlFrameHandler;
import io.github.lxien.orbien.client.transport.RealServerHandler;
import io.github.lxien.orbien.client.statemachine.agent.AgentContext;
import io.github.lxien.orbien.client.statemachine.agent.AgentStateMachineBuilder;
import io.github.lxien.orbien.client.statemachine.agent.AgentEvent;
import io.github.lxien.orbien.client.transport.UdpRealServerHandler;
import io.github.lxien.orbien.core.transport.IdleCheckHandler;
import io.github.lxien.orbien.core.transport.NettyConstants;
import io.github.lxien.orbien.core.transport.NettyEventLoopFactory;
import io.github.lxien.orbien.core.server.Lifecycle;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

/**
 * 代理客户端服务容器
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

        ControlFrameHandler controlTunnelHandler = new ControlFrameHandler(agentContext);
        agentContext.setControlFrameHandler(controlTunnelHandler);
        controlWorkerGroup = NettyEventLoopFactory.eventLoopGroup();
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
    }
}
