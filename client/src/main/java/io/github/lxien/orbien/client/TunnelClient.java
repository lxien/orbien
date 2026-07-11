package io.github.lxien.orbien.client;

import io.github.lxien.orbien.client.config.AppConfig;
import io.github.lxien.orbien.client.config.ConfigUtils;
import io.github.lxien.orbien.client.health.HealthCheckHolder;
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
import io.netty.util.concurrent.Future;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import lombok.Setter;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 代理客户端服务容器
 */
public final class TunnelClient implements Lifecycle {
    private final InternalLogger logger = InternalLoggerFactory.getInstance(TunnelClient.class);
    private final AppConfig config;
    private final AtomicBoolean stopped = new AtomicBoolean(false);
    private EventLoopGroup controlWorkerGroup;
    private EventLoopGroup serverWorkBootstrap;
    private AgentContext agentContext;
    /**
     * 独立进程模式下停止后退出 JVM。
     */
    @Setter
    private volatile boolean exitJvmOnStop;

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
        agentContext.setAgentIdentity(new AgentIdentity(config.getAgentType()));

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
        stopInternal(false);
    }

    /**
     * Shutdown hook 入口，仅释放资源。
     */
    public void shutdownGracefully() {
        stopInternal(true);
    }

    private void stopInternal(boolean fromShutdownHook) {
        if (!stopped.compareAndSet(false, true)) {
            return;
        }
        if (agentContext != null) {
            agentContext.markShuttingDown();
        }
        try {
            HealthCheckHolder.shutdown();
        } catch (IllegalStateException ignored) {
            // 尚未初始化
        }

        Future<?> controlShutdown = null;
        Future<?> serverShutdown = null;
        if (controlWorkerGroup != null) {
            controlShutdown = controlWorkerGroup.shutdownGracefully(0, 2, TimeUnit.SECONDS);
        }
        if (serverWorkBootstrap != null) {
            serverShutdown = serverWorkBootstrap.shutdownGracefully(0, 2, TimeUnit.SECONDS);
        }

        if (exitJvmOnStop && !fromShutdownHook) {
            logger.info("客户端已停止，退出进程");
            scheduleProcessExit(controlShutdown, serverShutdown);
        }
    }

    /**
     * 强退等场景必须在独立线程退出：若在 Netty EventLoop 上 System.exit，
     * shutdown hook 会再次 shutdownGracefully，与 EventLoop 线程互相等待导致死锁。
     */
    private void scheduleProcessExit(Future<?> controlShutdown, Future<?> serverShutdown) {
        Thread exitThread = new Thread(() -> {
            awaitQuietly(controlShutdown, 3);
            awaitQuietly(serverShutdown, 3);
            Runtime.getRuntime().halt(0);
        }, "orbien-exit");
        exitThread.setDaemon(false);
        exitThread.start();
    }

    private static void awaitQuietly(Future<?> future, long timeoutSeconds) {
        if (future == null) {
            return;
        }
        try {
            future.await(timeoutSeconds, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
