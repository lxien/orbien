package io.github.lxien.orbien.server.transport.bridge;

import io.github.lxien.orbien.core.transport.*;
import io.github.lxien.orbien.server.statemachine.stream.StreamContext;
import io.github.lxien.orbien.server.statemachine.stream.StreamEvent;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

public class DirectTunnelBridge implements TunnelBridge {
    private final InternalLogger logger = InternalLoggerFactory.getInstance(DirectTunnelBridge.class);
    private final StreamContext streamContext;
    private final Channel visitor;
    private final Channel tunnel;

    public DirectTunnelBridge(StreamContext streamContext) {
        this.streamContext = streamContext;
        this.visitor = streamContext.getVisitor();
        this.tunnel = streamContext.getTunnelEntry().getChannel();
    }

    @Override
    public void open() {
        ChannelPipeline pipeline = tunnel.pipeline();
        PipelineConfigure.removeControlHandler(tunnel);
        if (pipeline.get(NettyConstants.IDLE_CHECK_HANDLER) == null) {
            pipeline.addLast(NettyConstants.IDLE_CHECK_HANDLER, new IdleCheckHandler());
        }
        pipeline.addLast(NettyConstants.DIRECT_TUNNEL_BRIDGE_HANDLER,new SimpleChannelInboundHandler<ByteBuf>() {
            @Override
            protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) {
                streamContext.forwardToRemote(msg);
            }

            @Override
            public void channelWritabilityChanged(ChannelHandlerContext ctx) {
                logger.warn("隧道可写状态发生变化：{}", ctx.channel().isWritable());
                Channel tunnel = ctx.channel();
                boolean writable = tunnel.isWritable();
                if (writable) {
                    logger.debug("设置流 {} 为可读状态", streamContext.getStreamId());
                    visitor.config().setOption(ChannelOption.AUTO_READ, true);
                    visitor.read();
                }
                ctx.fireChannelWritabilityChanged();
            }

            @Override
            public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
                logger.error("独立隧道传输发生异常", cause);
            }
        });
        logger.debug("独立隧道流已打开成功， 处理器: {}", pipeline.names());
    }

    @Override
    public void forwardToLocal(ByteBuf payload) {
        if (!tunnel.isActive()) {
            logger.error("隧道没有激活：streamId={}", streamContext.getStreamId());
            streamContext.fireEvent(StreamEvent.STREAM_LOCAL_CLOSE);
            return;
        }
        payload.retain();
        tunnel.writeAndFlush(payload).addListener((ChannelFutureListener) f -> {
            logger.debug("流 {} 引用计数为：{}", streamContext.getStreamId(), payload.refCnt());
            if (f.isSuccess()) {
                logger.debug("数据转发到内网成功：streamId={}", streamContext.getStreamId());
            } else {
                logger.debug("数据转发到内网失败：streamId={}", streamContext.getStreamId(), f.cause());
                streamContext.fireEvent(StreamEvent.STREAM_LOCAL_CLOSE);
            }
        });
    }

    @Override
    public void forwardToRemote(ByteBuf payload) {
        if (!visitor.isActive()) {
            logger.error("访问者通道没有激活：streamId={}", streamContext.getStreamId());
            streamContext.fireEvent(StreamEvent.STREAM_LOCAL_CLOSE);
            return;
        }
        payload.retain();
        visitor.writeAndFlush(payload).addListener((ChannelFutureListener) f -> {
            logger.debug("流 {} 引用计数为：{}", streamContext.getStreamId(), payload.refCnt());
            if (f.isSuccess()) {
                logger.debug("数据转发给访问者成功：streamId={}", streamContext.getStreamId());
            } else {
                logger.debug("数据转发给访问者失败：streamId={}", streamContext.getStreamId(), f.cause());
                streamContext.fireEvent(StreamEvent.STREAM_LOCAL_CLOSE);
            }
        });
    }
}