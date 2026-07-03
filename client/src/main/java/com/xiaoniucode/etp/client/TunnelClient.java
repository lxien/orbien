package com.xiaoniucode.etp.client;

import com.xiaoniucode.etp.client.config.AppConfig;
import com.xiaoniucode.etp.client.config.ConfigUtils;
import com.xiaoniucode.etp.client.identity.AgentIdentity;
import com.xiaoniucode.etp.client.transport.ControlFrameHandler;
import com.xiaoniucode.etp.client.transport.ControlIdleCheckHandler;
import com.xiaoniucode.etp.client.transport.HeartbeatHandler;
import com.xiaoniucode.etp.client.transport.RealServerHandler;
import com.xiaoniucode.etp.client.statemachine.agent.AgentContext;
import com.xiaoniucode.etp.client.statemachine.agent.AgentStateMachineBuilder;
import com.xiaoniucode.etp.client.statemachine.agent.AgentEvent;
import com.xiaoniucode.etp.client.transport.connection.DirectPool;
import com.xiaoniucode.etp.client.transport.connection.MultiplexPool;
import com.xiaoniucode.etp.core.codec.TMSPCodec;
import com.xiaoniucode.etp.core.transport.IdleCheckHandler;
import com.xiaoniucode.etp.client.transport.UdpRealServerHandler;
import com.xiaoniucode.etp.core.transport.NettyConstants;
import com.xiaoniucode.etp.core.transport.NettyEventLoopFactory;
import com.xiaoniucode.etp.core.server.Lifecycle;
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
