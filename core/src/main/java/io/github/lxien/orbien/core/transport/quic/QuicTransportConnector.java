package io.github.lxien.orbien.core.transport.quic;
import io.github.lxien.orbien.core.enums.TransportProtocol;

import io.github.lxien.orbien.core.domain.transport.QuicProtocolConfig;
import io.github.lxien.orbien.core.transport.AttributeKeys;
import io.github.lxien.orbien.core.transport.NettyEventLoopFactory;
import io.github.lxien.orbien.core.transport.api.*;
import io.github.lxien.orbien.core.transport.pipeline.TmspPipelineConfigurer;
import io.github.lxien.orbien.core.transport.session.NettyTransportSession;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.incubator.codec.quic.*;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;

public class QuicTransportConnector implements TransportConnector {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(QuicTransportConnector.class);

    @Override
    public TransportProtocol protocol() {
        return TransportProtocol.QUIC;
    }

    @Override
    public CompletableFuture<TransportSession> connect(TransportConnectOptions options) {
        CompletableFuture<TransportSession> result = new CompletableFuture<>();
        TransportEndpoint endpoint = options.getEndpoint();
        QuicProtocolConfig quicConfig = options.getQuicConfig() != null
                ? options.getQuicConfig()
                : new QuicProtocolConfig();

        try {
            QuicSslContext quicSslContext = QuicSslSupport.buildClientContext(options.getTlsConfig());
            logger.debug("[传输] QUIC 客户端连接发起 endpoint={}:{} maxIdleTimeoutMs={}",
                    endpoint.getHost(), endpoint.getPort(), quicConfig.getMaxIdleTimeoutMs());

            QuicClientCodecBuilder codecBuilder = new QuicClientCodecBuilder();
            codecBuilder.sslContext(quicSslContext);
            codecBuilder.maxIdleTimeout(quicConfig.getMaxIdleTimeoutMs(), java.util.concurrent.TimeUnit.MILLISECONDS);
            codecBuilder.initialMaxData(quicConfig.getInitialMaxData());
            codecBuilder.initialMaxStreamDataBidirectionalLocal(quicConfig.getInitialMaxStreamData());
            codecBuilder.initialMaxStreamDataBidirectionalRemote(quicConfig.getInitialMaxStreamData());
            codecBuilder.initialMaxStreamsBidirectional(quicConfig.getInitialMaxStreamsBidi());

            EventLoopGroup group = options.getEventLoopGroup();
            Bootstrap datagramBootstrap = new Bootstrap()
                    .group(group)
                    .channel(NettyEventLoopFactory.datagramChannelClass())
                    .handler(codecBuilder.build());

            datagramBootstrap.bind(new InetSocketAddress(0)).addListener((ChannelFutureListener) bindListener -> {
                if (!bindListener.isSuccess()) {
                    result.completeExceptionally(bindListener.cause());
                    return;
                }
                Channel datagramChannel = bindListener.channel();
                logger.debug("[传输] QUIC UDP 通道已绑定 local={}", datagramChannel.localAddress());

                QuicChannel.newBootstrap(datagramChannel)
                        .handler(new ChannelInitializer<QuicChannel>() {
                            @Override
                            protected void initChannel(QuicChannel ch) {
                                ch.attr(AttributeKeys.QUIC_DATAGRAM).set(datagramChannel);
                                ch.attr(AttributeKeys.TRANSPORT_PROTOCOL).set(TransportProtocol.QUIC);
                                logger.debug("[传输] QUIC 连接已建立 remote={} local={}",
                                        ch.remoteAddress(), ch.localAddress());
                                ch.closeFuture().addListener(f -> logger.debug(
                                        "[传输] QUIC 连接已关闭 remote={} success={}",
                                        ch.remoteAddress(), f.isSuccess()));
                            }
                        })
                        .streamHandler(new ChannelInitializer<QuicStreamChannel>() {
                            @Override
                            protected void initChannel(QuicStreamChannel streamChannel) {
                                logger.debug("[传输] QUIC 收到服务端发起的 stream streamId={}",
                                        streamChannel.streamId());
                            }
                        })
                        .remoteAddress(new InetSocketAddress(endpoint.getHost(), endpoint.getPort()))
                        .connect()
                        .addListener((GenericFutureListener<Future<QuicChannel>>) connectListener -> {
                            if (!connectListener.isSuccess()) {
                                logger.error("[传输] QUIC 连接失败 endpoint={}:{}",
                                        endpoint.getHost(), endpoint.getPort(), connectListener.cause());
                                datagramChannel.close();
                                result.completeExceptionally(connectListener.cause());
                                return;
                            }
                            QuicChannel quicChannel = connectListener.getNow();
                            quicChannel.createStream(QuicStreamType.BIDIRECTIONAL,
                                    new ChannelInitializer<QuicStreamChannel>() {
                                        @Override
                                        protected void initChannel(QuicStreamChannel streamChannel) {
                                            logger.debug("[传输] QUIC 数据隧道 stream 已创建 streamId={} role={}",
                                                    streamChannel.streamId(), options.getRole());
                                            streamChannel.attr(AttributeKeys.QUIC_CONNECTION).set(quicChannel);
                                            streamChannel.attr(AttributeKeys.QUIC_DATAGRAM).set(datagramChannel);
                                            streamChannel.attr(AttributeKeys.TRANSPORT_PROTOCOL)
                                                    .set(TransportProtocol.QUIC);
                                            streamChannel.closeFuture().addListener(f -> logger.debug(
                                                    "[传输] QUIC 数据隧道 stream 已关闭 streamId={} success={}",
                                                    streamChannel.streamId(), f.isSuccess()));
                                            TmspPipelineConfigurer.configureClientPipeline(
                                                    streamChannel,
                                                    TransportProtocol.QUIC,
                                                    options.getRole(),
                                                    options.getTlsConfig(),
                                                    null,
                                                    null,
                                                    endpoint.getHost()
                                            );
                                            if (options.getPipelineTailConfigurer() != null) {
                                                options.getPipelineTailConfigurer().accept(streamChannel.pipeline());
                                            }
                                        }
                                    }).addListener((GenericFutureListener<Future<QuicStreamChannel>>) streamListener -> {
                                if (!streamListener.isSuccess()) {
                                    logger.error("[传输] QUIC 创建数据 stream 失败", streamListener.cause());
                                    quicChannel.close();
                                    datagramChannel.close();
                                    result.completeExceptionally(streamListener.cause());
                                    return;
                                }
                                QuicStreamChannel streamChannel = streamListener.getNow();
                                logger.debug("[传输] QUIC 数据隧道就绪 streamId={} channelId={}",
                                        streamChannel.streamId(), streamChannel.id());
                                result.complete(new NettyTransportSession(streamChannel, TransportProtocol.QUIC));
                            });
                        });
            });
        } catch (Exception e) {
            logger.error("[传输] QUIC 客户端初始化失败", e);
            result.completeExceptionally(e);
        }
        return result;
    }
}
