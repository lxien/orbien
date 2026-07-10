package io.github.lxien.orbien.client.transport.bridge;

import io.github.lxien.orbien.client.statemachine.stream.StreamContext;
import io.github.lxien.orbien.client.statemachine.stream.StreamEvent;
import io.github.lxien.orbien.core.message.TMSPFrame;
import io.github.lxien.orbien.core.transport.TunnelBridge;
import io.github.lxien.orbien.core.transport.ChannelBufWriter;
import io.github.lxien.orbien.core.enums.TransportProtocol;
import io.github.lxien.orbien.core.transport.compress.TmspPayloadCompressor;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.socket.DatagramPacket;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.ImmediateEventExecutor;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.net.InetSocketAddress;

public class MultiplexTunnelBridge implements TunnelBridge {
    private final InternalLogger logger = InternalLoggerFactory.getInstance(MultiplexTunnelBridge.class);
    private final StreamContext streamContext;
    private final Channel tunnel;
    private final Channel server;

    public MultiplexTunnelBridge(StreamContext streamContext) {
        this.streamContext = streamContext;
        this.tunnel = streamContext.getTunnelEntry().getChannel();
        this.server = streamContext.getServer();
    }

    @Override
    public Future<Void> openAsync() {
        return ImmediateEventExecutor.INSTANCE.newSucceededFuture(null);
    }

    @Override
    public void forwardToLocal(ByteBuf payload) {
        forwardToLocal(payload, true);
    }

    @Override
    public void forwardToLocal(ByteBuf payload, boolean sharedWithInbound) {
        if (payload == null || !payload.isReadable()) {
            logger.debug("[传输] 忽略空载荷 streamId={}", streamContext.getStreamId());
            return;
        }
        int streamId = streamContext.getStreamId();
        if (!server.isActive()) {
            if (!sharedWithInbound) {
                ReferenceCountUtil.release(payload);
            }
            logger.debug("[传输] 内网通道未激活，关闭流 streamId={}", streamId);
            streamContext.fireEvent(StreamEvent.STREAM_LOCAL_CLOSE);
            return;
        }
        if (streamContext.isDatagram()) {
            InetSocketAddress target = new InetSocketAddress(streamContext.getLocalIp(), streamContext.getLocalPort());
            ByteBuf datagramBuf = sharedWithInbound ? payload.retain() : payload;
            DatagramPacket packet = new DatagramPacket(datagramBuf, target);
            server.writeAndFlush(packet).addListener((ChannelFutureListener) future -> {
                if (!future.isSuccess()) {
                    logger.warn("[传输] UDP 数据转发到内网失败 streamId={}", streamId, future.cause());
                }
            });
            return;
        }
        logger.debug("[传输] tunnel->local streamId={} bytes={}", streamId, payload.readableBytes());
        ChannelBufWriter.write(server, payload, sharedWithInbound);
    }

    @Override
    public void forwardToRemote(ByteBuf payload) {
        forwardToRemote(payload, true);
    }

    @Override
    public void forwardToRemote(ByteBuf payload, boolean sharedWithInbound) {
        if (payload == null || !payload.isReadable()) {
            return;
        }
        int streamId = streamContext.getStreamId();
        TransportProtocol protocol = streamContext.getTransportProtocol();
        if (!tunnel.isActive()) {
            if (!sharedWithInbound) {
                ReferenceCountUtil.release(payload);
            }
            logger.warn("[传输] 数据隧道未激活，关闭流 streamId={} protocol={} channelClass={}",
                    streamId, protocol != null ? protocol.getName() : "unknown",
                    tunnel.getClass().getSimpleName());
            streamContext.fireEvent(StreamEvent.STREAM_LOCAL_CLOSE);
            return;
        }
        if (sharedWithInbound) {
            payload.retain();
        }
        Runnable writeTask = () -> {
            TMSPFrame frame = TmspPayloadCompressor.encodeStreamData(
                    tunnel, streamId, payload, streamContext.resolveCompressAlgorithm());
            logger.debug("[传输] local->tunnel streamId={} protocol={} bytes={} channelClass={}",
                    streamId, protocol != null ? protocol.getName() : "unknown",
                    payload.readableBytes(), tunnel.getClass().getSimpleName());
            tunnel.writeAndFlush(frame).addListener((ChannelFutureListener) future -> {
                ReferenceCountUtil.release(payload);
                if (!future.isSuccess()) {
                    logger.warn("[传输] 数据转发到远程隧道失败 streamId={} protocol={}",
                            streamId, protocol != null ? protocol.getName() : "unknown", future.cause());
                    streamContext.fireEvent(StreamEvent.STREAM_LOCAL_CLOSE);
                }
            });
        };
        if (tunnel.eventLoop().inEventLoop()) {
            writeTask.run();
        } else {
            tunnel.eventLoop().execute(writeTask);
        }
    }
}
