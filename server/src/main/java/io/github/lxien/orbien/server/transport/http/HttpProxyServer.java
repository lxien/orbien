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

package io.github.lxien.orbien.server.transport.http;

import io.github.lxien.orbien.core.transport.IdleCheckHandler;
import io.github.lxien.orbien.core.transport.NettyConstants;
import io.github.lxien.orbien.core.server.Lifecycle;
import io.github.lxien.orbien.core.transport.NettyEventLoopFactory;
import io.github.lxien.orbien.server.config.AppConfig;
import io.github.lxien.orbien.server.configuration.SpringContextHolder;
import io.github.lxien.orbien.server.transport.UploadRateLimitHandler;
import io.github.lxien.orbien.server.transport.VisitorPipelineSupport;
import io.github.lxien.orbien.server.transport.VisitorInfoDecoder;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

/**
 * Http proxy server
 *
 * @author lxien
 */
public class HttpProxyServer implements Lifecycle {
    private final InternalLogger logger = InternalLoggerFactory.getInstance(HttpProxyServer.class);
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private final HttpVisitorHandler httpVisitorHandler;
    private final AppConfig appConfig;
    private final HttpIpCheckHandler httpIpCheckHandler;
    private final BasicAuthHandler basicAuthHandler;
    private final ForceHttpsRedirectHandler forceHttpsRedirectHandler;

    public HttpProxyServer(AppConfig config, HttpVisitorHandler httpVisitorHandler, HttpIpCheckHandler httpIpCheckHandler, BasicAuthHandler basicAuthHandler, ForceHttpsRedirectHandler forceHttpsRedirectHandler) {
        this.appConfig = config;
        this.httpVisitorHandler = httpVisitorHandler;
        this.httpIpCheckHandler = httpIpCheckHandler;
        this.basicAuthHandler = basicAuthHandler;
        this.forceHttpsRedirectHandler = forceHttpsRedirectHandler;
    }

    @Override
    @PostConstruct
    public void start() {
        try {
            int httpProxyPort = appConfig.getHttpProxyPort();
            UploadRateLimitHandler uploadRateLimitHandler = SpringContextHolder.getBean(UploadRateLimitHandler.class);
            bossGroup = NettyEventLoopFactory.eventLoopGroup(1);
            workerGroup = NettyEventLoopFactory.eventLoopGroup();
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            serverBootstrap.group(bossGroup, workerGroup)
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    .childOption(ChannelOption.TCP_NODELAY, true)
                    .channel(NettyEventLoopFactory.serverSocketChannelClass())
                    .childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel sc) {
                            ChannelPipeline pipeline = sc.pipeline();
                            VisitorPipelineSupport.prependProxyProtocol(pipeline, appConfig.getProxyProtocol());
                            pipeline.addLast(new IdleCheckHandler());
                            pipeline.addLast(new VisitorInfoDecoder());
                            pipeline.addLast(forceHttpsRedirectHandler);
                            pipeline.addLast(new HeaderInjectDecoder());
                            pipeline.addLast(httpIpCheckHandler);
                            pipeline.addLast(uploadRateLimitHandler);
                            pipeline.addLast(basicAuthHandler);
                            pipeline.addLast(NettyConstants.HTTP_VISITOR_HANDLER, httpVisitorHandler);
                        }
                    });
            serverBootstrap.bind(httpProxyPort).syncUninterruptibly().get();
            logger.debug("http proxy server started on port {}", httpProxyPort);
        } catch (Exception e) {
            logger.error("http proxy server start error!", e);
        }
    }

    @Override
    @PreDestroy
    public void stop() {
        logger.debug("清理 HTTP 代理线程资源");
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
    }
}
