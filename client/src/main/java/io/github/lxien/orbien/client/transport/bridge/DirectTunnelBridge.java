package io.github.lxien.orbien.client.transport.bridge;

import io.github.lxien.orbien.client.statemachine.stream.StreamContext;
import io.github.lxien.orbien.client.statemachine.stream.StreamEvent;
import io.github.lxien.orbien.core.transport.*;
import io.github.lxien.orbien.core.transport.NettyConstants;
import io.github.lxien.orbien.core.transport.PipelineConfigure;
import io.github.lxien.orbien.core.transport.TunnelBridge;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

public class DirectTunnelBridge implements TunnelBridge {
    private final InternalLogger logger = InternalLoggerFactory.getInstance(DirectTunnelBridge.class);
    private final StreamContext streamContext;
    private final Channel tunnel;
    private final Channel server;

    public DirectTunnelBridge(StreamContext streamContext) {
        this.streamContext = streamContext;
        this.tunnel = streamContext.getTunnelEntry().getChannel();
        this.server = streamContext.getServer();
    }

    @Override
    public void open() {
        ChannelPipeline pipeline = tunnel.pipeline();
        PipelineConfigure.removeControlHandler(tunnel);
        pipeline.addLast(NettyConstants.DIRECT_TUNNEL_BRIDGE_HANDLER,new SimpleChannelInboundHandler<ByteBuf>() {
            @Override
            protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) {
                streamContext.forwardToLocal(msg);
            }

            @Override
            public void channelWritabilityChanged(ChannelHandlerContext ctx) {
                logger.debug("隧道可写状态发生变化：{}", ctx.channel().isWritable());
                Channel tunnel = ctx.channel();
                boolean writable = tunnel.isWritable();
                if (writable) {
                    server.config().setOption(ChannelOption.AUTO_READ, true);
                }
                ctx.fireChannelWritabilityChanged();
            }
            @Override
            public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
                logger.error("独立隧道传输发生异常",cause);
            }
        });
    }

    @Override
    public void forwardToLocal(ByteBuf payload) {
        if (!server.isActive()) {
            logger.error("目标服务连接已断开，关闭流：streamId={}", streamContext.getStreamId());
            streamContext.fireEvent(StreamEvent.STREAM_LOCAL_CLOSE);
            return;
        }
        server.writeAndFlush(payload.retain()).addListener((ChannelFutureListener) f -> {
            logger.debug("流 {} 引用计数为：{}", streamContext.getStreamId(), payload.refCnt());
            if (f.isSuccess()) {
                logger.debug("数据成功转发给真实服务成功");
            } else {
                logger.debug("数据转发给真实服务失败",f.cause());
                streamContext.fireEvent(StreamEvent.STREAM_LOCAL_CLOSE);
            }
        });
    }

    @Override
    public void forwardToRemote(ByteBuf payload) {
        if (!tunnel.isActive()) {
            logger.error("隧道没有激活，关闭流：streamId={}", streamContext.getStreamId());
            streamContext.fireEvent(StreamEvent.STREAM_LOCAL_CLOSE);
            return;
        }
        tunnel.writeAndFlush(payload.retain()).addListener((ChannelFutureListener) f -> {
            logger.debug("流 {} 引用计数为：{}", streamContext.getStreamId(), payload.refCnt());
            if (f.isSuccess()) {
                logger.debug("数据成功转发到远程成功");
            } else {
                logger.debug("数据转发到远程失败",f.cause());
            }
        });
    }
}