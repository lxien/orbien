/*
 *
 *  *    Copyright 2026 lxien
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

package io.github.lxien.orbien.server.transport.https;

import io.github.lxien.orbien.core.server.Lifecycle;
import io.github.lxien.orbien.core.transport.IdleCheckHandler;
import io.github.lxien.orbien.core.transport.NettyConstants;
import io.github.lxien.orbien.core.transport.NettyEventLoopFactory;
import io.github.lxien.orbien.server.config.AppConfig;
import io.github.lxien.orbien.server.service.ProxyConfigService;
import io.github.lxien.orbien.server.transport.VisitorInfoDecoder;
import io.github.lxien.orbien.server.transport.VisitorPipelineSupport;
import io.github.lxien.orbien.server.transport.file.FileShareDispatchHandler;
import io.github.lxien.orbien.server.transport.http.BasicAuthHandler;
import io.github.lxien.orbien.server.transport.http.HeaderInjectDecoder;
import io.github.lxien.orbien.server.transport.http.HeaderRewriteRequestDecoder;
import io.github.lxien.orbien.server.transport.http.HttpIpCheckHandler;
import io.github.lxien.orbien.server.transport.http.HttpVisitorHandler;
import io.github.lxien.orbien.server.vhost.DomainRegistry;
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
 * Https proxy server
 *
 * @author lxien
 */
public class HttpsProxyServer implements Lifecycle {
    private final InternalLogger logger = InternalLoggerFactory.getInstance(HttpsProxyServer.class);
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private final HttpVisitorHandler httpVisitorHandler;
    private final AppConfig appConfig;
    private final HttpIpCheckHandler httpIpCheckHandler;
    private final BasicAuthHandler basicAuthHandler;
    private final TlsCertificateManager tlsCertificateManager;
    private final FileShareDispatchHandler fileShareDispatchHandler;
    private final ProxyConfigService proxyConfigService;
    private final DomainRegistry domainRegistry;

    public HttpsProxyServer(AppConfig config,
                            HttpVisitorHandler httpVisitorHandler,
                            HttpIpCheckHandler httpIpCheckHandler,
                            BasicAuthHandler basicAuthHandler,
                            TlsCertificateManager tlsCertificateManager,
                            FileShareDispatchHandler fileShareDispatchHandler,
                            ProxyConfigService proxyConfigService,
                            DomainRegistry domainRegistry) {
        this.appConfig = config;
        this.httpVisitorHandler = httpVisitorHandler;
        this.httpIpCheckHandler = httpIpCheckHandler;
        this.basicAuthHandler = basicAuthHandler;
        this.tlsCertificateManager = tlsCertificateManager;
        this.fileShareDispatchHandler = fileShareDispatchHandler;
        this.proxyConfigService = proxyConfigService;
        this.domainRegistry = domainRegistry;
    }

    @Override
    @PostConstruct
    public void start() {
        try {
            int httpsProxyPort = appConfig.getHttpsProxyPort();
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
                            VisitorPipelineSupport.prependProxyProtocol(pipeline, appConfig.getProxyProtocol());
                            pipeline.addLast(new SniHandler(tlsCertificateManager::getSslContext));
                            pipeline.addLast(new IdleCheckHandler());
                            pipeline.addLast(new VisitorInfoDecoder());
                            pipeline.addLast(new HeaderInjectDecoder());
                            pipeline.addLast(new HeaderRewriteRequestDecoder(proxyConfigService, domainRegistry, "https"));
                            pipeline.addLast(httpIpCheckHandler);
                            pipeline.addLast(basicAuthHandler);
                            pipeline.addLast(fileShareDispatchHandler);
                            pipeline.addLast(NettyConstants.HTTP_VISITOR_HANDLER, httpVisitorHandler);
                        }
                    });
            serverBootstrap.bind(httpsProxyPort).syncUninterruptibly().get();
            logger.debug("HTTPS 隧道代理服务启动成功，端口为：{}", httpsProxyPort);
        } catch (Exception e) {
            logger.error("HTTPS 隧道代理服务启动失败!", e);
        }
    }

    @Override
    @PreDestroy
    public void stop() {
        logger.debug("清理 HTTPS 代理线程资源");
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
    }
}
