package io.github.lxien.orbien.core.transport.tcp;
import io.github.lxien.orbien.core.enums.TransportProtocol;

import io.github.lxien.orbien.core.transport.NettyEventLoopFactory;
import io.github.lxien.orbien.core.transport.api.*;
import io.github.lxien.orbien.core.transport.pipeline.TmspPipelineConfigurer;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.ssl.OptionalSslHandler;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

public class TcpTransportListener implements TransportListener {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(TcpTransportListener.class);

    private Channel serverChannel;
    private SessionAcceptor acceptor;
    private TransportBindOptions bindOptions;

    @Override
    public TransportProtocol protocol() {
        return TransportProtocol.TCP;
    }

    @Override
    public void bind(TransportBindOptions options, SessionAcceptor acceptor) {
        this.bindOptions = options;
        this.acceptor = acceptor;
    }

    @Override
    public void start() {
        if (bindOptions == null || acceptor == null) {
            throw new IllegalStateException("请先调用 bind()");
        }
        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(bindOptions.getBossGroup(), bindOptions.getWorkerGroup())
                .channel(NettyEventLoopFactory.serverSocketChannelClass())
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .childOption(ChannelOption.TCP_NODELAY, true)
                .childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        // TCP 服务端使用 OptionalSslHandler 协商 TLS，不可再叠加 SslHandler
                        boolean optionalTls = bindOptions.getSslContext() != null;
                        if (optionalTls) {
                            ch.pipeline().addLast(new OptionalSslHandler(bindOptions.getSslContext()));
                        }
                        TmspPipelineConfigurer.configureServerPipeline(
                                ch,
                                TransportProtocol.TCP,
                                null,
                                optionalTls ? null : bindOptions.getSslContext(),
                                bindOptions.getWebSocketConfig()
                        );
                        if (bindOptions.getPipelineConfigurer() != null) {
                            bindOptions.getPipelineConfigurer().accept(ch, ch.pipeline());
                        }
                    }
                });
        try {
            ChannelFuture future = bootstrap.bind(bindOptions.getAddr(), bindOptions.getPort()).sync();
            serverChannel = future.channel();
            logger.info("TCP 传输监听已启动 {}:{}", bindOptions.getAddr(), bindOptions.getPort());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("TCP 传输监听启动失败", e);
        }
    }

    @Override
    public void stop() {
        if (serverChannel != null) {
            serverChannel.close();
        }
    }
}
