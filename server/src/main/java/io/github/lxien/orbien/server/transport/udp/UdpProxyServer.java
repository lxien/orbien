package io.github.lxien.orbien.server.transport.udp;

import io.github.lxien.orbien.core.server.Lifecycle;
import io.github.lxien.orbien.core.transport.NettyConstants;
import io.github.lxien.orbien.core.transport.NettyEventLoopFactory;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.socket.DatagramChannel;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.Getter;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * UDP 代理服务启动、停止、管理
 */
public final class UdpProxyServer implements Lifecycle {
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(UdpProxyServer.class);
    @Getter
    private Bootstrap serverBootstrap;
    private EventLoopGroup workerGroup;
    private final AtomicBoolean init = new AtomicBoolean(false);
    private final UdpVisitorHandler udpVisitorHandler;
    private final UdpIpCheckHandler udpIpCheckHandler;
    private final UdpTimeAccessHandler udpTimeAccessHandler;

    public UdpProxyServer(UdpVisitorHandler udpVisitorHandler,
                          UdpIpCheckHandler udpIpCheckHandler,
                          UdpTimeAccessHandler udpTimeAccessHandler) {
        this.udpVisitorHandler = udpVisitorHandler;
        this.udpIpCheckHandler = udpIpCheckHandler;
        this.udpTimeAccessHandler = udpTimeAccessHandler;
    }

    @Override
    @PostConstruct
    public void start() {
        if (init.get()) {
            return;
        }
        workerGroup = NettyEventLoopFactory.eventLoopGroup();
        serverBootstrap = new Bootstrap();
        serverBootstrap.group(workerGroup)
                .channel(NettyEventLoopFactory.datagramChannelClass())
                .option(ChannelOption.SO_BROADCAST, false)
                .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .handler(new ChannelInitializer<DatagramChannel>() {
                    @Override
                    protected void initChannel(DatagramChannel channel) {
                        ChannelPipeline pipeline = channel.pipeline();
                        pipeline.addLast(udpIpCheckHandler);
                        pipeline.addLast(udpTimeAccessHandler);
                        pipeline.addLast(NettyConstants.UDP_VISITOR_HANDLER, udpVisitorHandler);
                    }
                });
        init.set(true);
        logger.debug("UDP 代理服务初始化成功");
    }

    @Override
    @PreDestroy
    public void stop() {
        if (!init.get()) {
            logger.warn("尚未初始化 UDP 服务");
            return;
        }
        logger.debug("清理 UDP 代理线程资源");
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }
    }
}
