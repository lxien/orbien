package io.github.lxien.orbien.client.transport;

import io.github.lxien.orbien.client.statemachine.stream.StreamContext;
import io.github.lxien.orbien.client.statemachine.stream.StreamEvent;
import io.github.lxien.orbien.client.statemachine.stream.StreamManager;
import io.github.lxien.orbien.core.transport.TunnelEntry;
import io.github.lxien.orbien.core.utils.ChannelUtils;
import io.netty.channel.*;
import io.netty.channel.socket.DatagramPacket;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.util.Optional;

/**
 * 内网 UDP 服务数据读写
 */
public class UdpRealServerHandler extends SimpleChannelInboundHandler<DatagramPacket> {
    private final InternalLogger logger = InternalLoggerFactory.getInstance(UdpRealServerHandler.class);

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, DatagramPacket packet) {
        Channel server = ctx.channel();
        Optional<StreamContext> streamCtx = StreamManager.getStreamContext(server);
        streamCtx.ifPresent(streamContext -> {
            TunnelEntry tunnelEntry = streamContext.getTunnelEntry();
            if (tunnelEntry == null) {
                return;
            }
            Channel tunnel = tunnelEntry.getChannel();
            if (!tunnel.isWritable()) {
                logger.debug("隧道不可写，暂停从 UDP 服务读取，streamId={}", streamContext.getStreamId());
                if (tunnelEntry.getTunnelType().isMultiplex()) {
                    StreamManager.addPausedStreamId(tunnel, streamContext.getStreamId());
                }
                return;
            }
            streamContext.forwardToRemote(packet.content());
        });
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        logger.error("内网 UDP 服务通道异常", cause);
        Channel server = ctx.channel();
        StreamManager.getStreamContext(server).ifPresent(streamContext -> {
            streamContext.fireEvent(StreamEvent.STREAM_LOCAL_CLOSE);
        });
        ChannelUtils.closeOnFlush(server);
    }
}
