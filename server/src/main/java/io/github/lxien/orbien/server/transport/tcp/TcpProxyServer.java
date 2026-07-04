/*
 *    Copyright 2026 lxien
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package io.github.lxien.orbien.server.transport.tcp;

import io.github.lxien.orbien.core.transport.IdleCheckHandler;
import io.github.lxien.orbien.core.transport.NettyConstants;
import io.github.lxien.orbien.core.server.Lifecycle;
import io.github.lxien.orbien.core.transport.NettyEventLoopFactory;
import io.github.lxien.orbien.core.notify.EventBus;
import io.github.lxien.orbien.server.configuration.SpringContextHolder;
import io.github.lxien.orbien.server.transport.UploadRateLimitHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.Getter;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * TCP服务启动、停止、管理
 *
 * @author liuxin
 */
public final class TcpProxyServer implements Lifecycle {
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(TcpProxyServer.class);
    @Getter
    private ServerBootstrap serverBootstrap;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private final AtomicBoolean init = new AtomicBoolean(false);
    private final TcpVisitorHandler tcpVisitorHandler;
    private final EventBus eventBus;
    private final TcpIpCheckHandler tcpIpCheckHandler;

    public TcpProxyServer(TcpVisitorHandler tcpVisitorHandler, TcpIpCheckHandler tcpIpCheckHandler, EventBus eventBus) {
        this.tcpVisitorHandler = tcpVisitorHandler;
        this.eventBus = eventBus;
        this.tcpIpCheckHandler = tcpIpCheckHandler;
    }

    @Override
    @PostConstruct
    public void start() {
        if (init.get()) {
            return;
        }
        bossGroup = NettyEventLoopFactory.eventLoopGroup(1);
        workerGroup = NettyEventLoopFactory.eventLoopGroup();
        serverBootstrap = new ServerBootstrap();
        UploadRateLimitHandler uploadRateLimitHandler = SpringContextHolder.getBean(UploadRateLimitHandler.class);
        serverBootstrap.group(bossGroup, workerGroup)
                .channel(NettyEventLoopFactory.serverSocketChannelClass())
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .childOption(ChannelOption.TCP_NODELAY, true)
                .childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel sc) {
                        ChannelPipeline pipeline = sc.pipeline();

                        pipeline.addLast(new IdleCheckHandler());
                        pipeline.addLast(tcpIpCheckHandler);
                        pipeline.addLast(uploadRateLimitHandler);
                        pipeline.addLast(NettyConstants.TCP_VISITOR_HANDLER, tcpVisitorHandler);
                    }
                });
        init.set(true);
        logger.debug("TCP 代理服务初始化成功");
    }

    @Override
    @PreDestroy
    public void stop() {
        if (!init.get()) {
            logger.warn("尚未初始化TCP 服务");
            return;
        }
        logger.debug("清理 TCP 代理线程资源");
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }
    }
}
