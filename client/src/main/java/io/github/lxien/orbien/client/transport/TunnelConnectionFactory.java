package io.github.lxien.orbien.client.transport;

import io.github.lxien.orbien.client.config.AppConfig;
import io.github.lxien.orbien.client.statemachine.agent.AgentContext;
import io.github.lxien.orbien.core.codec.TMSPCodec;
import io.github.lxien.orbien.core.transport.IdleCheckHandler;
import io.github.lxien.orbien.core.transport.NettyEventLoopFactory;
import io.github.lxien.orbien.core.transport.NettyConstants;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.compression.SnappyFrameDecoder;
import io.netty.handler.codec.compression.SnappyFrameEncoder;
import io.netty.handler.ssl.SslHandler;

import java.util.function.Consumer;

/**
 * TCP 连接创建工具类
 */
public class TunnelConnectionFactory {

    /**
     * 创建数据隧道连接
     *
     * @param agentContext Agent 上下文
     * @param config       应用配置
     * @param isEncrypt    是否加密
     * @param callback     连接创建成功后的回调
     */
    public static void createConnection(AgentContext agentContext,
                                        AppConfig config,
                                        boolean isEncrypt,
                                        Consumer<Channel> callback) {
        Bootstrap dataBootstrap = new Bootstrap()
                .group(agentContext.getControlWorkerGroup())
                .channel(NettyEventLoopFactory.socketChannelClass())
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel sc) {
                        if (isEncrypt && agentContext.getTlsContext() != null) {
                            SslHandler sslHandler = agentContext.getTlsContext().newHandler(
                                    sc.alloc(), config.getServerAddr(), config.getServerPort());
                            sc.pipeline().addLast(NettyConstants.TLS_HANDLER, sslHandler);
                        }
                        sc.pipeline()
                                .addLast(new SnappyFrameEncoder())
                                .addLast(new SnappyFrameDecoder())
                                .addLast(NettyConstants.TMSP_CODEC, TMSPCodec.create(10 * 1024 * 1024))
                                .addLast(NettyConstants.IDLE_CHECK_HANDLER, new IdleCheckHandler())
                                .addLast(NettyConstants.CONTROL_FRAME_HANDLER, agentContext.getControlFrameHandler());
                    }
                });

        dataBootstrap.connect(config.getServerAddr(), config.getServerPort())
                .addListener((ChannelFutureListener) future -> {
                    if (future.isSuccess()) {
                        Channel tunnel = future.channel();
                        callback.accept(tunnel);
                    } else {
                        callback.accept(null);
                    }
                });
    }
}
