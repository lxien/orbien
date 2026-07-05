package io.github.lxien.orbien.core.transport.direct;

import io.github.lxien.orbien.core.codec.TMSPCodec;
import io.github.lxien.orbien.core.transport.AttributeKeys;
import io.github.lxien.orbien.core.transport.NettyConstants;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.compression.SnappyFrameDecoder;
import io.netty.handler.codec.compression.SnappyFrameEncoder;
import io.netty.util.concurrent.Future;

import java.util.function.Consumer;

/**
 * 独立隧道（Direct）生命周期：在 TMSP 控制栈与原始 TCP 透传栈之间切换。
 * <p>
 * 所有 pipeline 变更必须在隧道 channel 所属 EventLoop 上执行。
 */
public final class DirectTunnelLifecycle {

    private static final int DEFAULT_MAX_FRAME = 10 * 1024 * 1024;

    private DirectTunnelLifecycle() {
    }

    /**
     * 切换为原始字节透传模式，并挂载 bridge handler。
     */
    public static Future<Void> enablePassthrough(Channel tunnel, ChannelHandler bridgeHandler) {
        return toVoidFuture(runOnLoop(tunnel, () -> {
            ChannelPipeline pipeline = tunnel.pipeline();
            removeIfPresent(pipeline, NettyConstants.DIRECT_TUNNEL_BRIDGE_HANDLER);
            removeIfPresent(pipeline, NettyConstants.TMSP_CODEC);
            removeIfPresent(pipeline, NettyConstants.CONTROL_FRAME_HANDLER);
            removeIfPresent(pipeline, NettyConstants.CONTROL_IDLE_CHECK_HANDLER);
            removeIfPresent(pipeline, NettyConstants.IDLE_CHECK_HANDLER);
            removeIfPresent(pipeline, NettyConstants.SNAPPY_ENCODER);
            removeIfPresent(pipeline, NettyConstants.SNAPPY_DECODER);
            removeIfPresent(pipeline, NettyConstants.DOWNLOAD_RATE_LIMIT_HANDLER);
            pipeline.addLast(NettyConstants.DIRECT_TUNNEL_BRIDGE_HANDLER, bridgeHandler);
            tunnel.attr(AttributeKeys.DIRECT_PASSTHROUGH).set(Boolean.TRUE);
        }));
    }

    /**
     * 流关闭后恢复 TMSP 控制栈，以便独立隧道归还连接池后仍可复用。
     */
    public static Future<Void> restoreControlStack(Channel tunnel, Consumer<ChannelPipeline> tailConfigurer) {
        return toVoidFuture(runOnLoop(tunnel, () -> {
            ChannelPipeline pipeline = tunnel.pipeline();
            removeIfPresent(pipeline, NettyConstants.DIRECT_TUNNEL_BRIDGE_HANDLER);
            if (pipeline.get(NettyConstants.TMSP_CODEC) != null) {
                tunnel.attr(AttributeKeys.DIRECT_PASSTHROUGH).set(Boolean.FALSE);
                return;
            }
            String anchor = sslAnchor(pipeline);
            pipeline.addAfter(anchor, NettyConstants.SNAPPY_ENCODER, new SnappyFrameEncoder());
            pipeline.addAfter(NettyConstants.SNAPPY_ENCODER, NettyConstants.SNAPPY_DECODER, new SnappyFrameDecoder());
            pipeline.addAfter(NettyConstants.SNAPPY_DECODER, NettyConstants.TMSP_CODEC,
                    TMSPCodec.create(DEFAULT_MAX_FRAME));
            if (tailConfigurer != null) {
                tailConfigurer.accept(pipeline);
            }
            tunnel.attr(AttributeKeys.DIRECT_PASSTHROUGH).set(Boolean.FALSE);
        }));
    }

    public static boolean isPassthroughActive(Channel tunnel) {
        if (tunnel == null) {
            return false;
        }
        Boolean active = tunnel.attr(AttributeKeys.DIRECT_PASSTHROUGH).get();
        return Boolean.TRUE.equals(active)
                || tunnel.pipeline().get(NettyConstants.DIRECT_TUNNEL_BRIDGE_HANDLER) != null;
    }

    public static void runOnTunnelLoop(Channel tunnel, Runnable task) {
        if (tunnel.eventLoop().inEventLoop()) {
            task.run();
        } else {
            tunnel.eventLoop().execute(task);
        }
    }

    private static Future<?> runOnLoop(Channel tunnel, Runnable task) {
        if (tunnel.eventLoop().inEventLoop()) {
            try {
                task.run();
                return tunnel.newSucceededFuture();
            } catch (Throwable t) {
                return tunnel.newFailedFuture(t);
            }
        }
        return tunnel.eventLoop().submit(task);
    }

    @SuppressWarnings("unchecked")
    private static Future<Void> toVoidFuture(Future<?> future) {
        return (Future<Void>) future;
    }

    private static void removeIfPresent(ChannelPipeline pipeline, String name) {
        if (pipeline.get(name) != null) {
            pipeline.remove(name);
        }
    }

    private static String sslAnchor(ChannelPipeline pipeline) {
        if (pipeline.get(NettyConstants.TLS_HANDLER) != null) {
            return NettyConstants.TLS_HANDLER;
        }
        for (String name : pipeline.names()) {
            if (name.contains("Ssl") || name.contains("ssl") || name.contains("OptionalSsl")) {
                return name;
            }
        }
        return pipeline.firstContext().name();
    }
}
