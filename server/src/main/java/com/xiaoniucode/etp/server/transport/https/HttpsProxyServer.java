/*
 *
 *  *    Copyright 2026 xiaoniucode
 *  *
 *  *    Licensed under the Apache License, Version 2.0 (the "License");
 *  *    you may not use this file except in compliance with the License.
 *  *    You may obtain a copy of the License at
 *  *
 *  *        http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  *    Unless required by applicable law or agreed to in writing, software
 *  *    distributed under the License is distributed on an "AS IS" BASIS,
 *  *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  *    See the License for the specific language governing permissions and
 *  *    limitations under the License.
 *
 */

package com.xiaoniucode.etp.server.transport.https;

import com.xiaoniucode.etp.core.notify.EventBus;
import com.xiaoniucode.etp.core.server.Lifecycle;
import com.xiaoniucode.etp.core.transport.NettyConstants;
import com.xiaoniucode.etp.core.transport.NettyEventLoopFactory;
import com.xiaoniucode.etp.server.config.AppConfig;
import com.xiaoniucode.etp.server.configuration.SpringContextHolder;
import com.xiaoniucode.etp.server.transport.UploadRateLimitHandler;
import com.xiaoniucode.etp.server.transport.VisitorInfoDecoder;
import com.xiaoniucode.etp.server.transport.http.BasicAuthHandler;
import com.xiaoniucode.etp.server.transport.http.HttpIpCheckHandler;
import com.xiaoniucode.etp.server.transport.http.HttpVisitorHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.ssl.SniHandler;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

/**
 * Http proxy server
 *
 * @author xiaoniucode
 */
public class HttpsProxyServer implements Lifecycle {
    private final InternalLogger logger = InternalLoggerFactory.getInstance(HttpsProxyServer.class);
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private final HttpVisitorHandler httpVisitorHandler;
    private final AppConfig appConfig;
    private final EventBus eventBus;
    private final HttpIpCheckHandler httpIpCheckHandler;
    private final BasicAuthHandler basicAuthHandler;
    private final SslCertificateManager sslCertificateManager;

    public HttpsProxyServer(AppConfig config, HttpVisitorHandler httpVisitorHandler, HttpIpCheckHandler httpIpCheckHandler, BasicAuthHandler basicAuthHandler, EventBus eventBus, SslCertificateManager sslCertificateManager) {
        this.appConfig = config;
        this.httpVisitorHandler = httpVisitorHandler;
        this.eventBus = eventBus;
        this.httpIpCheckHandler = httpIpCheckHandler;
        this.basicAuthHandler = basicAuthHandler;
        this.sslCertificateManager = sslCertificateManager;
    }

    @Override
    @PostConstruct
    public void start() {
        try {
            int httpsProxyPort = appConfig.getHttpsProxyPort();
            UploadRateLimitHandler uploadRateLimitHandler = SpringContextHolder.getBean(UploadRateLimitHandler.class);
            bossGroup = NettyEventLoopFactory.eventLoopGroup(1);
            workerGroup = NettyEventLoopFactory.eventLoopGroup();
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            serverBootstrap.group(bossGroup, workerGroup)
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    .channel(NettyEventLoopFactory.serverSocketChannelClass())
                    .childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel sc) {
                            ChannelPipeline pipeline = sc.pipeline();
                            pipeline.addLast(new SniHandler(sslCertificateManager::getSslContext));
                            //pipeline.addLast(new IdleCheckHandler());
                            pipeline.addLast(new VisitorInfoDecoder());
                           // pipeline.addLast(new HeaderInjectDecoder());
                          //  pipeline.addLast(httpIpCheckHandler);
                         //   pipeline.addLast(uploadRateLimitHandler);
                           // pipeline.addLast(basicAuthHandler);
                            pipeline.addLast(NettyConstants.HTTP_VISITOR_HANDLER, httpVisitorHandler);
                        }
                    });
            serverBootstrap.bind(httpsProxyPort).syncUninterruptibly().get();
            logger.debug("HTTPS 隧道代理服务启动成功，端口为：{}", httpsProxyPort);
        } catch (Exception e) {
            logger.error("HTTPS 隧道代理服务启动失败", e);
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
