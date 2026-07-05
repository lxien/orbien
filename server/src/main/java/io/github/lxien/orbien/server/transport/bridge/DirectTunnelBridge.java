package io.github.lxien.orbien.server.transport.bridge;

import io.github.lxien.orbien.core.transport.TunnelBridge;
import io.github.lxien.orbien.core.transport.direct.DirectTunnelLifecycle;
import io.github.lxien.orbien.server.statemachine.stream.StreamContext;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.util.concurrent.Future;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

public class DirectTunnelBridge implements TunnelBridge {
    private final InternalLogger logger = InternalLoggerFactory.getInstance(DirectTunnelBridge.class);
    private final StreamContext streamContext;
    private final Channel visitor;
    private final Channel tunnel;
    private final ChannelHandler bridgeHandler;

    public DirectTunnelBridge(StreamContext streamContext) {
        this.streamContext = streamContext;
        this.visitor = streamContext.getVisitor();
        this.tunnel = streamContext.getTunnelEntry().getChannel();
        this.bridgeHandler = new SimpleChannelInboundHandler<ByteBuf>() {
            @Override
            protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) {
                streamContext.forwardToRemote(msg);
            }

            @Override
            public void channelWritabilityChanged(ChannelHandlerContext ctx) {
                if (ctx.channel().isWritable() && visitor.isActive()) {
                    visitor.config().setOption(ChannelOption.AUTO_READ, true);
                    visitor.read();
                }
                ctx.fireChannelWritabilityChanged();
            }

            @Override
            public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
                logger.error("独立隧道传输发生异常 streamId={}", streamContext.getStreamId(), cause);
                StreamForwardHelper.abortAndClose(streamContext, logger,
                        "独立隧道异常 streamId=" + streamContext.getStreamId(), cause);
            }
        };
    }

    @Override
    public Future<Void> openAsync() {
        return DirectTunnelLifecycle.enablePassthrough(tunnel, bridgeHandler);
    }

    @Override
    public void forwardToLocal(ByteBuf payload) {
        if (!StreamForwardHelper.shouldForward(streamContext)) {
            return;
        }
        if (!tunnel.isActive()) {
            StreamForwardHelper.abortAndClose(streamContext, logger,
                    "隧道没有激活：streamId=" + streamContext.getStreamId(), null);
            return;
        }
        writeOnLoop(tunnel, payload.retain(), success -> {
            if (!success) {
                StreamForwardHelper.abortAndClose(streamContext, logger,
                        "数据转发到内网失败：streamId=" + streamContext.getStreamId(), null);
            }
        });
    }

    @Override
    public void forwardToRemote(ByteBuf payload) {
        if (!StreamForwardHelper.shouldForward(streamContext)) {
            return;
        }
        if (!visitor.isActive()) {
            StreamForwardHelper.abortAndClose(streamContext, logger,
                    "访问者通道没有激活：streamId=" + streamContext.getStreamId(), null);
            return;
        }
        writeOnLoop(visitor, payload.retain(), success -> {
            if (!success) {
                StreamForwardHelper.abortAndClose(streamContext, logger,
                        "数据转发给访问者失败：streamId=" + streamContext.getStreamId(), null);
            }
        });
    }

    private void writeOnLoop(Channel channel, ByteBuf payload, java.util.function.Consumer<Boolean> listener) {
        int bytes = payload.readableBytes();
        Runnable writeTask = () -> channel.writeAndFlush(payload).addListener((ChannelFutureListener) f -> {
            if (f.isSuccess()) {
                logger.debug("数据转发成功 streamId={} bytes={}", streamContext.getStreamId(), bytes);
            } else {
                logger.warn("数据转发失败 streamId={}", streamContext.getStreamId(), f.cause());
            }
            listener.accept(f.isSuccess());
        });
        DirectTunnelLifecycle.runOnTunnelLoop(channel, writeTask);
    }
}
