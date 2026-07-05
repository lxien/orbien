package io.github.lxien.orbien.server.transport.bridge;

import io.github.lxien.orbien.core.message.TMSP;
import io.github.lxien.orbien.core.message.TMSPFrame;
import io.github.lxien.orbien.core.transport.TunnelBridge;
import io.github.lxien.orbien.core.transport.TunnelEntry;
import io.github.lxien.orbien.core.enums.TransportProtocol;
import io.github.lxien.orbien.server.statemachine.stream.StreamContext;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.ImmediateEventExecutor;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

public class MultiplexTunnelBridge implements TunnelBridge {
    private final InternalLogger logger = InternalLoggerFactory.getInstance(MultiplexTunnelBridge.class);
    private final StreamContext streamContext;
    private final TunnelEntry tunnelEntry;
    private final Channel visitor;

    public MultiplexTunnelBridge(StreamContext streamContext) {
        this.streamContext = streamContext;
        this.tunnelEntry = streamContext.getTunnelEntry();
        this.visitor = streamContext.getVisitor();
    }

    @Override
    public Future<Void> openAsync() {
        return ImmediateEventExecutor.INSTANCE.newSucceededFuture(null);
    }

    @Override
    public void forwardToLocal(ByteBuf payload) {
        if (payload == null || !payload.isReadable()) {
            logger.debug("[传输] 忽略空载荷 streamId={}", streamContext.getStreamId());
            return;
        }
        if (!StreamForwardHelper.shouldForward(streamContext)) {
            return;
        }
        Channel tunnel = tunnelEntry.getChannel();
        int streamId = streamContext.getStreamId();
        TransportProtocol protocol = tunnelEntry.getProtocol();
        if (!tunnel.isActive()) {
            StreamForwardHelper.abortAndClose(streamContext, logger,
                    "[传输] 数据隧道未激活，转发失败 streamId=" + streamId
                            + " protocol=" + (protocol != null ? protocol.getName() : "unknown"), null);
            return;
        }
        payload.retain();
        Runnable writeTask = () -> {
            if (!StreamForwardHelper.shouldForward(streamContext) || !tunnel.isActive()) {
                ReferenceCountUtil.release(payload);
                StreamForwardHelper.abortAndClose(streamContext, logger,
                        "[传输] 数据隧道已失效，放弃写入 streamId=" + streamId, null);
                return;
            }
            writeToTunnel(tunnel, streamId, protocol, payload);
        };
        if (tunnel.eventLoop().inEventLoop()) {
            writeTask.run();
        } else {
            tunnel.eventLoop().execute(writeTask);
        }
    }

    private void writeToTunnel(Channel tunnel, int streamId, TransportProtocol protocol, ByteBuf payload) {
        TMSPFrame frame = new TMSPFrame(streamId, TMSP.MSG_STREAM_DATA, payload);
        logger.debug("[传输] visitor->tunnel streamId={} protocol={} bytes={} channelClass={} refCnt={}",
                streamId, protocol != null ? protocol.getName() : "unknown",
                payload.readableBytes(), tunnel.getClass().getSimpleName(), payload.refCnt());
        tunnel.writeAndFlush(frame).addListener((ChannelFutureListener) future -> {
            if (!future.isSuccess()) {
                StreamForwardHelper.abortAndClose(streamContext, logger,
                        "[传输] 数据转发到内网失败 streamId=" + streamId
                                + " protocol=" + (protocol != null ? protocol.getName() : "unknown"),
                        future.cause());
            } else {
                logger.debug("[传输] 数据转发到内网成功 streamId={} protocol={}",
                        streamId, protocol != null ? protocol.getName() : "unknown");
            }
        });
    }

    @Override
    public void forwardToRemote(ByteBuf payload) {
        int streamId = streamContext.getStreamId();
        if (!StreamForwardHelper.shouldForward(streamContext)) {
            return;
        }
        if (!visitor.isActive()) {
            StreamForwardHelper.abortAndClose(streamContext, logger,
                    "[传输] 访问者通道未激活，转发失败 streamId=" + streamId, null);
            return;
        }
        writePayloadToVisitor(payload, streamId);
    }

    private void writePayloadToVisitor(ByteBuf payload, int streamId) {
        payload.retain();
        visitor.writeAndFlush(payload).addListener((ChannelFutureListener) future -> {
            if (!future.isSuccess()) {
                StreamForwardHelper.abortAndClose(streamContext, logger,
                        "[传输] 数据转发到访问者失败 streamId=" + streamId, future.cause());
            } else {
                logger.debug("[传输] 数据转发到访问者成功 streamId={} bytes={}",
                        streamId, payload.readableBytes());
            }
        });
    }
}
