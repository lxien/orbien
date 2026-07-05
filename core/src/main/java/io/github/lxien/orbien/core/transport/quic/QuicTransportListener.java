package io.github.lxien.orbien.core.transport.quic;
import io.github.lxien.orbien.core.enums.TransportProtocol;

import io.github.lxien.orbien.core.domain.TlsConfig;
import io.github.lxien.orbien.core.domain.transport.QuicProtocolConfig;
import io.github.lxien.orbien.core.transport.AttributeKeys;
import io.github.lxien.orbien.core.transport.NettyEventLoopFactory;
import io.github.lxien.orbien.core.transport.api.*;
import io.github.lxien.orbien.core.transport.pipeline.TmspPipelineConfigurer;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.incubator.codec.quic.*;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

public class QuicTransportListener implements TransportListener {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(QuicTransportListener.class);

    private Channel serverChannel;
    private TransportBindOptions bindOptions;

    @Override
    public TransportProtocol protocol() {
        return TransportProtocol.QUIC;
    }

    @Override
    public void bind(TransportBindOptions options, SessionAcceptor acceptor) {
        this.bindOptions = options;
    }

    @Override
    public void start() {
        if (bindOptions == null) {
            throw new IllegalStateException("请先调用 bind()");
        }
        QuicProtocolConfig quicConfig = bindOptions.getQuicConfig() != null
                ? bindOptions.getQuicConfig()
                : new QuicProtocolConfig();
        try {
            TlsConfig tlsConfig = quicConfig.getTlsConfig();
            if (tlsConfig == null) {
                tlsConfig = new TlsConfig(true);
            }
            QuicSslContext quicSslContext = QuicSslSupport.buildServerContext(tlsConfig);
            logger.debug("[传输] QUIC 服务端启动 maxIdleTimeoutMs={} maxStreamsBidi={}",
                    quicConfig.getMaxIdleTimeoutMs(), quicConfig.getInitialMaxStreamsBidi());

            QuicServerCodecBuilder codecBuilder = new QuicServerCodecBuilder();
            codecBuilder.sslContext(quicSslContext);
            codecBuilder.maxIdleTimeout(quicConfig.getMaxIdleTimeoutMs(), TimeUnit.MILLISECONDS);
            codecBuilder.initialMaxData(quicConfig.getInitialMaxData());
            codecBuilder.initialMaxStreamDataBidirectionalLocal(quicConfig.getInitialMaxStreamData());
            codecBuilder.initialMaxStreamDataBidirectionalRemote(quicConfig.getInitialMaxStreamData());
            codecBuilder.initialMaxStreamsBidirectional(quicConfig.getInitialMaxStreamsBidi());
            codecBuilder.handler(new ChannelInitializer<QuicChannel>() {
                @Override
                protected void initChannel(QuicChannel ch) {
                    ch.attr(AttributeKeys.TRANSPORT_PROTOCOL).set(TransportProtocol.QUIC);
                    logger.debug("[传输] QUIC 服务端接受连接 remote={} local={}",
                            ch.remoteAddress(), ch.localAddress());
                    ch.closeFuture().addListener(f -> logger.debug(
                            "[传输] QUIC 服务端连接关闭 remote={} success={}",
                            ch.remoteAddress(), f.isSuccess()));
                }
            });
            codecBuilder.streamHandler(new ChannelInitializer<QuicStreamChannel>() {
                @Override
                protected void initChannel(QuicStreamChannel streamChannel) {
                    logger.debug("[传输] QUIC 服务端接受 stream streamId={} remote={}",
                            streamChannel.streamId(), streamChannel.parent().remoteAddress());
                    streamChannel.attr(AttributeKeys.QUIC_CONNECTION).set(streamChannel.parent());
                    streamChannel.attr(AttributeKeys.TRANSPORT_PROTOCOL).set(TransportProtocol.QUIC);
                    streamChannel.config().setAutoRead(true);
                    streamChannel.closeFuture().addListener(f -> logger.debug(
                            "[传输] QUIC 服务端 stream 关闭 streamId={} success={}",
                            streamChannel.streamId(), f.isSuccess()));
                    TmspPipelineConfigurer.configureServerPipeline(
                            streamChannel,
                            TransportProtocol.QUIC,
                            null,
                            null,
                            null
                    );
                    if (bindOptions.getPipelineConfigurer() != null) {
                        bindOptions.getPipelineConfigurer().accept(streamChannel, streamChannel.pipeline());
                    }
                }
            });

            Bootstrap bootstrap = new Bootstrap()
                    .group(bindOptions.getWorkerGroup())
                    .channel(NettyEventLoopFactory.datagramChannelClass())
                    .handler(codecBuilder.build());

            ChannelFuture future = bootstrap.bind(new InetSocketAddress(bindOptions.getAddr(), bindOptions.getPort())).sync();
            serverChannel = future.channel();
            logger.info("QUIC 传输监听已启动 {}:{}", bindOptions.getAddr(), bindOptions.getPort());
        } catch (Exception e) {
            throw new IllegalStateException("QUIC 传输监听启动失败", e);
        }
    }

    @Override
    public void stop() {
        if (serverChannel != null) {
            serverChannel.close();
        }
    }
}
