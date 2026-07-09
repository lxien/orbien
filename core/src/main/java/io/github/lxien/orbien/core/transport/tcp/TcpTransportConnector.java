package io.github.lxien.orbien.core.transport.tcp;
import io.github.lxien.orbien.core.enums.TransportProtocol;

import io.github.lxien.orbien.core.transport.NettyEventLoopFactory;
import io.github.lxien.orbien.core.transport.api.*;
import io.github.lxien.orbien.core.transport.pipeline.TmspPipelineConfigurer;
import io.github.lxien.orbien.core.transport.session.NettyTransportSession;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;

import java.util.concurrent.CompletableFuture;

public class TcpTransportConnector implements TransportConnector {

    @Override
    public TransportProtocol protocol() {
        return TransportProtocol.TCP;
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
                                TransportProtocol.TCP,
                                options.getRole(),
                                options.getTlsConfig(),
                                options.getSslContext(),
                                options.getWebSocketConfig(),
                                endpoint.getHost(),
                                options.isConnectionEncrypt()
                        );
                        if (options.getPipelineTailConfigurer() != null) {
                            options.getPipelineTailConfigurer().accept(ch.pipeline());
                        }
                    }
                });

        ChannelFuture connectFuture = bootstrap.connect(endpoint.getHost(), endpoint.getPort());
        connectFuture.addListener((ChannelFutureListener) f -> {
            if (f.isSuccess()) {
                Channel channel = f.channel();
                future.complete(new NettyTransportSession(channel, TransportProtocol.TCP));
            } else {
                future.completeExceptionally(f.cause());
            }
        });
        return future;
    }
}
