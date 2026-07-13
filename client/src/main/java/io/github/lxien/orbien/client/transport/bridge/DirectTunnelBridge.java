package io.github.lxien.orbien.client.transport.bridge;

import io.github.lxien.orbien.client.statemachine.stream.StreamContext;
import io.github.lxien.orbien.client.statemachine.stream.StreamEvent;
import io.github.lxien.orbien.client.statemachine.stream.StreamState;
import io.github.lxien.orbien.core.transport.TunnelBridge;
import io.github.lxien.orbien.core.transport.direct.DirectTunnelLifecycle;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.concurrent.Future;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

public class DirectTunnelBridge implements TunnelBridge {
    private final InternalLogger logger = InternalLoggerFactory.getInstance(DirectTunnelBridge.class);
    private final StreamContext streamContext;
    private final Channel tunnel;
    private final Channel server;
    private final ChannelHandler bridgeHandler;

    public DirectTunnelBridge(StreamContext streamContext) {
        this.streamContext = streamContext;
        this.tunnel = streamContext.getTunnelEntry().getChannel();
        this.server = streamContext.getServer();
        this.bridgeHandler = new SimpleChannelInboundHandler<ByteBuf>() {
            @Override
            protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) {
                streamContext.forwardToLocal(msg);
            }

            @Override
            public void channelWritabilityChanged(ChannelHandlerContext ctx) {
                if (ctx.channel().isWritable() && server.isActive()) {
                    streamContext.resumeBackendRead(StreamContext.BACKEND_PAUSE_BACKPRESSURE);
                }
                ctx.fireChannelWritabilityChanged();
            }

            @Override
            public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
                logger.error("独立隧道传输发生异常 streamId={}", streamContext.getStreamId(), cause);
                streamContext.fireEvent(StreamEvent.STREAM_LOCAL_CLOSE);
            }
        };
    }

    @Override
    public Future<Void> openAsync() {
        return DirectTunnelLifecycle.enablePassthrough(tunnel, bridgeHandler,
                streamContext.resolveCompressAlgorithm());
    }

    @Override
    public void forwardToLocal(ByteBuf payload) {
        forwardToLocal(payload, true);
    }

    @Override
    public void forwardToLocal(ByteBuf payload, boolean sharedWithInbound) {
        if (payload == null || !payload.isReadable()) {
            if (!sharedWithInbound) {
                ReferenceCountUtil.release(payload);
            }
            return;
        }
        if (!server.isActive()) {
            if (!sharedWithInbound) {
                ReferenceCountUtil.release(payload);
            }
            logger.error("目标服务连接已断开，关闭流：streamId={}", streamContext.getStreamId());
            streamContext.fireEvent(StreamEvent.STREAM_LOCAL_CLOSE);
            return;
        }
        writeOnLoop(server, payload, sharedWithInbound, success -> {
            if (!success) {
                streamContext.fireEvent(StreamEvent.STREAM_LOCAL_CLOSE);
            }
        });
    }

    @Override
    public void forwardToRemote(ByteBuf payload) {
        forwardToRemote(payload, true);
    }

    @Override
    public void forwardToRemote(ByteBuf payload, boolean sharedWithInbound) {
        if (payload == null || !payload.isReadable()) {
            if (!sharedWithInbound) {
                ReferenceCountUtil.release(payload);
            }
            return;
        }
        if (!tunnel.isActive()) {
            if (!sharedWithInbound) {
                ReferenceCountUtil.release(payload);
            }
            logger.error("隧道没有激活，关闭流：streamId={}", streamContext.getStreamId());
            streamContext.fireEvent(StreamEvent.STREAM_LOCAL_CLOSE);
            return;
        }
        writeOnLoop(tunnel, payload, sharedWithInbound, success -> {
            if (!success) {
                streamContext.fireEvent(StreamEvent.STREAM_LOCAL_CLOSE);
            }
        });
    }

    private void writeOnLoop(Channel channel, ByteBuf payload, boolean sharedWithInbound,
                             java.util.function.Consumer<Boolean> listener) {
        final ByteBuf outbound = sharedWithInbound ? payload.retain() : payload;
        int bytes = outbound.readableBytes();
        // 投递前计数，避免 backend 断开时 pending 低估导致提前关流
        streamContext.beforeTunnelWrite();
        Runnable writeTask = () -> {
            if (streamContext.getState() == StreamState.CLOSED
                    || streamContext.getState() == StreamState.FAILED
                    || !channel.isActive()) {
                ReferenceCountUtil.safeRelease(outbound);
                streamContext.afterTunnelWrite();
                listener.accept(false);
                return;
            }
            channel.writeAndFlush(outbound).addListener((ChannelFutureListener) f -> {
                streamContext.afterTunnelWrite();
                if (!f.isSuccess() && outbound.refCnt() > 0) {
                    ReferenceCountUtil.safeRelease(outbound);
                }
                if (f.isSuccess()) {
                    logger.debug("流 {} 数据转发成功 bytes={}", streamContext.getStreamId(), bytes);
                } else {
                    logger.warn("流 {} 数据转发失败", streamContext.getStreamId(), f.cause());
                }
                listener.accept(f.isSuccess());
            });
        };
        DirectTunnelLifecycle.runOnTunnelLoop(channel, writeTask);
    }
}
