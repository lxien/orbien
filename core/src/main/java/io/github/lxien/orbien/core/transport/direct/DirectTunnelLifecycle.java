package io.github.lxien.orbien.core.transport.direct;

import io.github.lxien.orbien.core.transport.compress.CompressionType;
import io.github.lxien.orbien.core.codec.TMSPCodec;
import io.github.lxien.orbien.core.transport.AttributeKeys;
import io.github.lxien.orbien.core.transport.NettyConstants;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelPipeline;
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
     * 若代理开启压缩，在透传栈上增加分块压缩编解码器。
     */
    public static Future<Void> enablePassthrough(Channel tunnel, ChannelHandler bridgeHandler,
                                                 CompressionType compressAlgorithm) {
        CompressionType algorithm = compressAlgorithm != null ? compressAlgorithm : CompressionType.NONE;
        return toVoidFuture(runOnLoop(tunnel, () -> {
            ChannelPipeline pipeline = tunnel.pipeline();
            removeIfPresent(pipeline, NettyConstants.DIRECT_TUNNEL_BRIDGE_HANDLER);
            removeIfPresent(pipeline, NettyConstants.DIRECT_TUNNEL_COMPRESSION_DECODER);
            removeIfPresent(pipeline, NettyConstants.DIRECT_TUNNEL_COMPRESSION_ENCODER);
            removeIfPresent(pipeline, NettyConstants.TMSP_CODEC);
            removeIfPresent(pipeline, NettyConstants.CONTROL_FRAME_HANDLER);
            removeIfPresent(pipeline, NettyConstants.CONTROL_IDLE_CHECK_HANDLER);
            removeIfPresent(pipeline, NettyConstants.IDLE_CHECK_HANDLER);
            removeIfPresent(pipeline, NettyConstants.DOWNLOAD_RATE_LIMIT_HANDLER);
            if (algorithm.isCompressed()) {
                pipeline.addLast(NettyConstants.DIRECT_TUNNEL_COMPRESSION_DECODER,
                        new DirectTunnelCompressionDecoder(algorithm));
                pipeline.addLast(NettyConstants.DIRECT_TUNNEL_COMPRESSION_ENCODER,
                        new DirectTunnelCompressionEncoder(algorithm));
            }
            pipeline.addLast(NettyConstants.DIRECT_TUNNEL_BRIDGE_HANDLER, bridgeHandler);
            tunnel.attr(AttributeKeys.DIRECT_PASSTHROUGH).set(Boolean.TRUE);
        }));
    }

    /**
     * 流关闭后恢复 TMSP 控制栈，以便独立隧道归还连接池后仍可复用。
     * <p>
     * 若隧道已关闭或 pipeline 不可恢复（如协议切换并发关闭），静默完成，由调用方回收连接。
     */
    public static Future<Void> restoreControlStack(Channel tunnel, Consumer<ChannelPipeline> tailConfigurer) {
        return toVoidFuture(runOnLoop(tunnel, () -> {
            tunnel.attr(AttributeKeys.DIRECT_PASSTHROUGH).set(Boolean.FALSE);
            if (!tunnel.isOpen()) {
                return;
            }
            ChannelPipeline pipeline = tunnel.pipeline();
            if (pipeline.firstContext() == null) {
                return;
            }
            removeIfPresent(pipeline, NettyConstants.DIRECT_TUNNEL_BRIDGE_HANDLER);
            removeIfPresent(pipeline, NettyConstants.DIRECT_TUNNEL_COMPRESSION_DECODER);
            removeIfPresent(pipeline, NettyConstants.DIRECT_TUNNEL_COMPRESSION_ENCODER);
            if (pipeline.get(NettyConstants.TMSP_CODEC) != null) {
                return;
            }
            pipeline.addLast(NettyConstants.TMSP_CODEC, TMSPCodec.create(DEFAULT_MAX_FRAME));
            if (tailConfigurer != null) {
                tailConfigurer.accept(pipeline);
            }
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

    /**
     * 独立隧道是否仍可恢复控制栈（未关闭且 pipeline 可用）。
     */
    public static boolean canRestoreControlStack(Channel tunnel) {
        return tunnel != null && tunnel.isOpen() && tunnel.pipeline().firstContext() != null;
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

    /**
     * 在 TLS / WebSocket 解码栈之后挂载 TMSP 控制栈；透传后仅剩 head/tail 时返回 null，改用 addLast。
     */
    private static String findControlStackAnchor(ChannelPipeline pipeline) {
        if (pipeline.get(NettyConstants.WEBSOCKET_FRAME_CODEC) != null) {
            return NettyConstants.WEBSOCKET_FRAME_CODEC;
        }
        if (pipeline.get(NettyConstants.WEBSOCKET_HANDLER) != null) {
            return NettyConstants.WEBSOCKET_HANDLER;
        }
        if (pipeline.get(NettyConstants.TLS_HANDLER) != null) {
            return NettyConstants.TLS_HANDLER;
        }
        for (String name : pipeline.names()) {
            if (name.contains("Ssl") || name.contains("ssl") || name.contains("OptionalSsl")) {
                return name;
            }
        }
        String lastTransport = null;
        for (String name : pipeline.names()) {
            if (!isHeadOrTail(name)) {
                lastTransport = name;
            }
        }
        return lastTransport;
    }

    private static boolean isHeadOrTail(String name) {
        return name == null || name.contains("Head") || name.contains("Tail");
    }
}
