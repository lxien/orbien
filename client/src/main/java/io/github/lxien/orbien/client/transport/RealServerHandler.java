package io.github.lxien.orbien.client.transport;

import io.github.lxien.orbien.client.statemachine.stream.StreamContext;
import io.github.lxien.orbien.client.statemachine.stream.StreamEvent;
import io.github.lxien.orbien.client.statemachine.stream.StreamManager;
import io.github.lxien.orbien.core.transport.TunnelEntry;
import io.github.lxien.orbien.core.utils.ChannelUtils;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.util.internal.OutOfDirectMemoryError;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * 内网目标服务 → 隧道。
 */
public class RealServerHandler extends SimpleChannelInboundHandler<ByteBuf> {
    private final InternalLogger logger = InternalLoggerFactory.getInstance(RealServerHandler.class);

    /**
     * 已提交但未完成的隧道写超过该值则停读，避免等 isWritable=false 时已囤太多直连缓冲
     */
    private static final int MAX_PENDING_TUNNEL_WRITES = 2;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) {
        Channel server = ctx.channel();
        Optional<StreamContext> streamCtx = StreamManager.getStreamContext(server);
        streamCtx.ifPresent(streamContext -> {
            TunnelEntry tunnelEntry = streamContext.getTunnelEntry();
            Channel tunnel = tunnelEntry.getChannel();
            int pending = streamContext.getPendingTunnelWrites().get();
            if (!tunnel.isWritable() || pending >= MAX_PENDING_TUNNEL_WRITES) {
                logger.debug("隧道背压，暂停后端读取 streamId={} writable={} pending={}",
                        streamContext.getStreamId(), tunnel.isWritable(), pending);
                streamContext.pauseBackendRead(StreamContext.BACKEND_PAUSE_BACKPRESSURE);
                StreamManager.addPausedStreamId(tunnel, streamContext.getStreamId());
            }
            streamContext.forwardToRemote(msg);
        });
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        logger.debug("目标服务连接断开", ctx);
        Channel server = ctx.channel();
        Optional<StreamContext> streamCtx = StreamManager.getStreamContext(server);
        streamCtx.ifPresent(streamContext -> {
            logger.debug("目标服务连接断开，等待隧道数据发送完毕后关闭流 {}", streamContext.getStreamId());
            streamContext.markBackendDisconnected();
        });
        if (streamCtx.isEmpty()) {
            logger.debug("没有获取到真实服务流信息");
        }
        ctx.fireChannelInactive();
    }

    @Override
    public void channelWritabilityChanged(ChannelHandlerContext ctx) {
        // 后端通道自身可写性变化不强制恢复 AUTO_READ：
        // 限流 / 隧道背压暂停必须由对应 reason 清除后才能恢复。
        ctx.fireChannelWritabilityChanged();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        Channel server = ctx.channel();
        Optional<StreamContext> streamCtx = StreamManager.getStreamContext(server);
        if (isDirectMemoryPressure(cause)) {
            logger.error("目标服务读路径 direct memory 耗尽，暂停后端读取（不关流） streamId={}",
                    streamCtx.map(StreamContext::getStreamId).orElse(null), cause);
            streamCtx.ifPresent(streamContext -> {
                Channel tunnel = streamContext.getTunnelEntry() != null
                        ? streamContext.getTunnelEntry().getChannel()
                        : null;
                streamContext.pauseBackendRead(StreamContext.BACKEND_PAUSE_BACKPRESSURE);
                if (tunnel == null) {
                    return;
                }
                StreamManager.addPausedStreamId(tunnel, streamContext.getStreamId());
                if (tunnel.isWritable()) {
                    tunnel.eventLoop().schedule(() -> {
                        if (!streamContext.canResumeBackendRead()) {
                            return;
                        }
                        streamContext.resumeBackendRead(StreamContext.BACKEND_PAUSE_BACKPRESSURE);
                        StreamManager.removePausedStream(tunnel, streamContext.getStreamId());
                    }, 100, TimeUnit.MILLISECONDS);
                }
            });
            return;
        }
        logger.error("目标服务连接发生异常", cause);
        streamCtx.ifPresent(streamContext -> {
            logger.error("目标服务连接发生异常，关闭流：{}", streamContext.getStreamId());
            streamContext.fireEvent(StreamEvent.STREAM_LOCAL_CLOSE);
        });
        ChannelUtils.closeOnFlush(server);
        ctx.fireExceptionCaught(cause);
    }

    private static boolean isDirectMemoryPressure(Throwable cause) {
        for (Throwable c = cause; c != null; c = c.getCause()) {
            if (c instanceof OutOfDirectMemoryError || c instanceof OutOfMemoryError) {
                return true;
            }
        }
        return false;
    }
}
