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

package io.github.lxien.orbien.server.transport.socks5;

import io.github.lxien.orbien.core.server.Lifecycle;
import io.github.lxien.orbien.core.transport.IdleCheckHandler;
import io.github.lxien.orbien.core.transport.NettyConstants;
import io.github.lxien.orbien.core.transport.NettyEventLoopFactory;
import io.github.lxien.orbien.server.config.AppConfig;
import io.github.lxien.orbien.server.configuration.SpringContextHolder;
import io.github.lxien.orbien.server.transport.UploadRateLimitHandler;
import io.github.lxien.orbien.server.transport.VisitorPipelineSupport;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.Getter;

import java.util.concurrent.atomic.AtomicBoolean;

public final class Socks5ProxyServer implements Lifecycle {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(Socks5ProxyServer.class);

    @Getter
    private ServerBootstrap serverBootstrap;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private final AtomicBoolean init = new AtomicBoolean(false);
    private final Socks5HandshakeHandler handshakeHandler;
    private final Socks5RelayHandler relayHandler;
    private final Socks5IpCheckHandler ipCheckHandler;
    private final AppConfig appConfig;

    public Socks5ProxyServer(Socks5HandshakeHandler handshakeHandler,
                               Socks5RelayHandler relayHandler,
                               Socks5IpCheckHandler ipCheckHandler,
                               AppConfig appConfig) {
        this.handshakeHandler = handshakeHandler;
        this.relayHandler = relayHandler;
        this.ipCheckHandler = ipCheckHandler;
        this.appConfig = appConfig;
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
                        VisitorPipelineSupport.prependProxyProtocol(pipeline, appConfig.getProxyProtocol());
                        pipeline.addLast(new IdleCheckHandler());
                        pipeline.addLast(ipCheckHandler);
                        pipeline.addLast(uploadRateLimitHandler);
                        pipeline.addLast(NettyConstants.SOCKS5_HANDSHAKE_HANDLER, handshakeHandler);
                        pipeline.addLast(NettyConstants.SOCKS5_RELAY_HANDLER, relayHandler);
                    }
                });
        init.set(true);
        logger.debug("SOCKS5 代理服务初始化成功");
    }

    @Override
    @PreDestroy
    public void stop() {
        if (!init.get()) {
            return;
        }
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }
    }
}
