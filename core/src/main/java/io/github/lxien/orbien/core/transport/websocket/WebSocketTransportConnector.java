package io.github.lxien.orbien.core.transport.websocket;
import io.github.lxien.orbien.core.enums.TransportProtocol;

import io.github.lxien.orbien.core.transport.NettyEventLoopFactory;
import io.github.lxien.orbien.core.transport.api.*;
import io.github.lxien.orbien.core.transport.pipeline.TmspPipelineConfigurer;
import io.github.lxien.orbien.core.transport.session.NettyTransportSession;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.websocketx.WebSocketClientProtocolHandler;

import java.util.concurrent.CompletableFuture;

public class WebSocketTransportConnector implements TransportConnector {

    @Override
    public TransportProtocol protocol() {
        return TransportProtocol.WEBSOCKET;
    }

    @Override
    public CompletableFuture<TransportSession> connect(TransportConnectOptions options) {
        CompletableFuture<TransportSession> future = new CompletableFuture<>();
        TransportEndpoint endpoint = options.getEndpoint();

        Bootstrap bootstrap = new Bootstrap()
                .group(options.getEventLoopGroup())
                .channel(NettyEventLoopFactory.socketChannelClass())
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        TmspPipelineConfigurer.configureClientPipeline(
                                ch,
                                TransportProtocol.WEBSOCKET,
                                options.getRole(),
                                options.getTlsConfig(),
                                options.getSslContext(),
                                options.getWebSocketConfig(),
                                endpoint.getHost(),
                                true
                        );
                        ch.pipeline().addLast(new WebSocketHandshakeAwaiter(future));
                        if (options.getPipelineTailConfigurer() != null) {
                            options.getPipelineTailConfigurer().accept(ch.pipeline());
                        }
                    }
                });

        bootstrap.connect(endpoint.getHost(), endpoint.getPort()).addListener((ChannelFutureListener) f -> {
            if (!f.isSuccess()) {
                future.completeExceptionally(f.cause());
            }
        });
        return future;
    }

    /**
     * 等待 WebSocket 握手完成后再通知连接就绪，避免在 HTTP 握手阶段写入 TMSP 帧。
     */
    private static final class WebSocketHandshakeAwaiter extends ChannelInboundHandlerAdapter {
        private final CompletableFuture<TransportSession> future;

        private WebSocketHandshakeAwaiter(CompletableFuture<TransportSession> future) {
            this.future = future;
        }

        @Override
        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
            if (evt == WebSocketClientProtocolHandler.ClientHandshakeStateEvent.HANDSHAKE_COMPLETE) {
                if (!future.isDone()) {
                    future.complete(new NettyTransportSession(ctx.channel(), TransportProtocol.WEBSOCKET));
                }
            }
            ctx.fireUserEventTriggered(evt);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            if (!future.isDone()) {
                future.completeExceptionally(cause);
            }
            ctx.fireExceptionCaught(cause);
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) {
            if (!future.isDone()) {
                future.completeExceptionally(new IllegalStateException("WebSocket 连接在握手完成前关闭"));
            }
            ctx.fireChannelInactive();
        }
    }
}
