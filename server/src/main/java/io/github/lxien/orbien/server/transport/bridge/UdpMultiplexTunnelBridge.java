package io.github.lxien.orbien.server.transport.bridge;

import io.github.lxien.orbien.core.message.TMSP;
import io.github.lxien.orbien.core.message.TMSPFrame;
import io.github.lxien.orbien.core.transport.TunnelBridge;
import io.github.lxien.orbien.core.transport.TunnelEntry;
import io.github.lxien.orbien.server.statemachine.stream.StreamContext;
import io.github.lxien.orbien.server.statemachine.stream.StreamEvent;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.socket.DatagramPacket;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.ImmediateEventExecutor;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.net.InetSocketAddress;

/**
 * UDP 多路复用隧道桥接
 */
public class UdpMultiplexTunnelBridge implements TunnelBridge {
    private final InternalLogger logger = InternalLoggerFactory.getInstance(UdpMultiplexTunnelBridge.class);
    private final StreamContext streamContext;
    private final TunnelEntry tunnelEntry;
    private final Channel visitor;
    private final InetSocketAddress visitorAddress;

    public UdpMultiplexTunnelBridge(StreamContext streamContext) {
        this.streamContext = streamContext;
        this.tunnelEntry = streamContext.getTunnelEntry();
        this.visitor = streamContext.getVisitor();
        this.visitorAddress = streamContext.getVisitorAddress();
    }

    @Override
    public Future<Void> openAsync() {
        return ImmediateEventExecutor.INSTANCE.newSucceededFuture(null);
    }

    @Override
    public void forwardToLocal(ByteBuf payload) {
        Channel tunnel = tunnelEntry.getChannel();
        int streamId = streamContext.getStreamId();
        if (!tunnel.isActive()) {
            logger.debug("[UDP] 数据通道未激活，关闭流 streamId={}", streamId);
            streamContext.fireEvent(StreamEvent.STREAM_LOCAL_CLOSE);
            return;
        }
        payload.retain();
        TMSPFrame frame = new TMSPFrame(streamId, TMSP.MSG_STREAM_DATA, payload);
        tunnel.writeAndFlush(frame).addListener((ChannelFutureListener) future -> {
            if (!future.isSuccess()) {
                logger.debug("[UDP] 数据转发到内网失败 streamId={}", streamId, future.cause());
            }
        });
    }

    @Override
    public void forwardToRemote(ByteBuf payload) {
        int streamId = streamContext.getStreamId();
        if (!visitor.isActive() || visitorAddress == null) {
            logger.debug("[UDP] 访问者不可用，关闭流 streamId={}", streamId);
            streamContext.fireEvent(StreamEvent.STREAM_LOCAL_CLOSE);
            return;
        }
        DatagramPacket packet = new DatagramPacket(payload.retain(), visitorAddress);
        visitor.writeAndFlush(packet).addListener((ChannelFutureListener) future -> {
            if (!future.isSuccess()) {
                logger.debug("[UDP] 数据转发到访问者失败 streamId={}", streamId, future.cause());
            }
        });
    }
}
