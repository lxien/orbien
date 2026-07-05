package io.github.lxien.orbien.core.transport.pipeline;

import io.github.lxien.orbien.core.codec.TMSPCodec;
import io.github.lxien.orbien.core.domain.TlsConfig;
import io.github.lxien.orbien.core.domain.transport.WebSocketProtocolConfig;
import io.github.lxien.orbien.core.transport.AttributeKeys;
import io.github.lxien.orbien.core.transport.NettyConstants;
import io.github.lxien.orbien.core.transport.websocket.WebSocketBinaryFrameCodec;
import io.github.lxien.orbien.core.transport.api.PipelineRole;
import io.github.lxien.orbien.core.enums.TransportProtocol;
import io.netty.channel.Channel;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.compression.SnappyFrameDecoder;
import io.netty.handler.codec.compression.SnappyFrameEncoder;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketClientProtocolHandler;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.codec.http.websocketx.WebSocketVersion;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslHandler;

import java.net.URI;

public final class TmspPipelineConfigurer {

    private static final int DEFAULT_MAX_FRAME = 10 * 1024 * 1024;

    private TmspPipelineConfigurer() {
    }

    public static void configureClientPipeline(Channel channel,
                                               TransportProtocol protocol,
                                               PipelineRole role,
                                               TlsConfig tlsConfig,
                                               SslContext sslContext,
                                               WebSocketProtocolConfig wsConfig,
                                               String remoteHost) {
        ChannelPipeline pipeline = channel.pipeline();
        switch (protocol) {
            case TCP -> addTcpClient(pipeline, tlsConfig, sslContext, remoteHost);
            case WEBSOCKET -> {
                addWebSocketClient(pipeline, sslContext, wsConfig, remoteHost);
                pipeline.addLast(NettyConstants.WEBSOCKET_FRAME_CODEC, new WebSocketBinaryFrameCodec());
            }
            case QUIC -> {
                // QUIC TLS 由连接层处理
            }
            default -> throw new IllegalArgumentException("Unsupported protocol: " + protocol);
        }
        addTmspStack(pipeline);
        channel.attr(AttributeKeys.TRANSPORT_PROTOCOL).set(protocol);
    }

    public static void configureServerPipeline(Channel channel,
                                               TransportProtocol protocol,
                                               TlsConfig tlsConfig,
                                               SslContext sslContext,
                                               WebSocketProtocolConfig wsConfig) {
        ChannelPipeline pipeline = channel.pipeline();
        switch (protocol) {
            case TCP -> addTcpServer(pipeline, sslContext);
            case WEBSOCKET -> {
                addWebSocketServer(pipeline, sslContext, wsConfig);
                pipeline.addLast(NettyConstants.WEBSOCKET_FRAME_CODEC, new WebSocketBinaryFrameCodec());
            }
            case QUIC -> {
                // QUIC stream pipeline
            }
            default -> throw new IllegalArgumentException("Unsupported protocol: " + protocol);
        }
        addTmspStack(pipeline);
        channel.attr(AttributeKeys.TRANSPORT_PROTOCOL).set(protocol);
    }

    private static void addTcpClient(ChannelPipeline pipeline, TlsConfig tlsConfig, SslContext sslContext, String remoteHost) {
        if (shouldUseTls(tlsConfig, sslContext)) {
            SslHandler sslHandler = sslContext.newHandler(pipeline.channel().alloc(), remoteHost, 443);
            pipeline.addLast(NettyConstants.TLS_HANDLER, sslHandler);
        }
    }

    private static void addTcpServer(ChannelPipeline pipeline, SslContext sslContext) {
        if (sslContext != null) {
            pipeline.addLast(NettyConstants.TLS_HANDLER, sslContext.newHandler(pipeline.channel().alloc()));
        }
    }

    private static void addWebSocketClient(ChannelPipeline pipeline,
                                           SslContext sslContext,
                                           WebSocketProtocolConfig wsConfig,
                                           String remoteHost) {
        if (sslContext == null) {
            throw new IllegalStateException("WebSocket 传输必须启用 TLS");
        }
        pipeline.addLast(NettyConstants.TLS_HANDLER, sslContext.newHandler(pipeline.channel().alloc(), remoteHost, 443));
        pipeline.addLast(new HttpClientCodec());
        pipeline.addLast(new HttpObjectAggregator(DEFAULT_MAX_FRAME));
        String path = wsConfig != null ? wsConfig.getPath() : "/tunnel";
        WebSocketClientProtocolHandler wsHandler = new WebSocketClientProtocolHandler(
                URI.create("wss://" + remoteHost + path),
                WebSocketVersion.V13,
                null,
                true,
                null,
                DEFAULT_MAX_FRAME
        );
        pipeline.addLast(NettyConstants.WEBSOCKET_HANDLER, wsHandler);
    }

    private static void addWebSocketServer(ChannelPipeline pipeline,
                                           SslContext sslContext,
                                           WebSocketProtocolConfig wsConfig) {
        if (sslContext == null) {
            throw new IllegalStateException("WebSocket 传输必须启用 TLS");
        }
        pipeline.addLast(NettyConstants.TLS_HANDLER, sslContext.newHandler(pipeline.channel().alloc()));
        pipeline.addLast(new HttpServerCodec());
        pipeline.addLast(new HttpObjectAggregator(DEFAULT_MAX_FRAME));
        String path = wsConfig != null ? wsConfig.getPath() : "/tunnel";
        int maxFrame = wsConfig != null ? wsConfig.getMaxFrameSize() : DEFAULT_MAX_FRAME;
        pipeline.addLast(NettyConstants.WEBSOCKET_HANDLER,
                new WebSocketServerProtocolHandler(path, null, true, maxFrame));
    }

    private static void addTmspStack(ChannelPipeline pipeline) {
        pipeline.addLast(NettyConstants.SNAPPY_ENCODER, new SnappyFrameEncoder());
        pipeline.addLast(NettyConstants.SNAPPY_DECODER, new SnappyFrameDecoder());
        pipeline.addLast(NettyConstants.TMSP_CODEC, TMSPCodec.create(DEFAULT_MAX_FRAME));
    }

    private static boolean shouldUseTls(TlsConfig tlsConfig, SslContext sslContext) {
        if (sslContext == null) {
            return false;
        }
        return tlsConfig == null || tlsConfig.isEnabled();
    }
}
