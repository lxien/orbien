package com.xiaoniucode.etp.server.transport.udp;

import com.alibaba.cola.statemachine.StateMachine;
import com.xiaoniucode.etp.core.enums.ProtocolType;
import com.xiaoniucode.etp.core.transport.TunnelEntry;
import com.xiaoniucode.etp.core.transport.UdpSessionKey;
import com.xiaoniucode.etp.server.statemachine.stream.StreamContext;
import com.xiaoniucode.etp.server.statemachine.stream.StreamEvent;
import com.xiaoniucode.etp.server.statemachine.stream.StreamManager;
import com.xiaoniucode.etp.server.statemachine.stream.StreamState;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;
import java.util.Optional;

/**
 * 处理来自公网访问者的 UDP 请求
 */
@Component
@ChannelHandler.Sharable
public class UdpVisitorHandler extends SimpleChannelInboundHandler<DatagramPacket> {
    private static final long SESSION_IDLE_TIMEOUT_SECONDS = 60;
    private final InternalLogger logger = InternalLoggerFactory.getInstance(UdpVisitorHandler.class);
    @Autowired
    private StreamManager streamManager;
    @Autowired
    @Qualifier("streamStateMachine")
    private StateMachine<StreamState, StreamEvent, StreamContext> stateMachine;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, DatagramPacket packet) {
        Channel channel = ctx.channel();
        InetSocketAddress sender = packet.sender();
        int listenPort = ((InetSocketAddress) channel.localAddress()).getPort();
        UdpSessionKey sessionKey = UdpSessionKey.of(listenPort, sender);

        Optional<StreamContext> existing = streamManager.getStreamContextByUdpSession(sessionKey);
        if (existing.isPresent()) {
            StreamContext streamContext = existing.get();
            if (streamContext.getState() == StreamState.OPENED) {
                handleDatagram(streamContext, packet.content());
                streamManager.touchUdpSession(sessionKey);
                return;
            }
            logger.debug("[UDP] 会话 {} 流 {} 状态 {}，清理后重建",
                    sessionKey, streamContext.getStreamId(), streamContext.getState());
            streamManager.unregisterUdpSession(sessionKey);
        }

        openNewUdpStream(channel, sender, sessionKey, packet);
    }

    private void openNewUdpStream(Channel channel,
                                  InetSocketAddress sender,
                                  UdpSessionKey sessionKey,
                                  DatagramPacket packet) {
        logger.debug("[UDP] 收到新访问者请求 {} -> {}", sender,
                ((InetSocketAddress) channel.localAddress()).getPort());
        StreamContext streamContext = streamManager.createUdpStreamContext(channel, sender, stateMachine);
        streamContext.setProtocol(ProtocolType.UDP);
        streamContext.setDatagram(true);
        streamContext.setMultiplex(true);
        streamContext.setStreamManager(streamManager);
        streamContext.setVisitorAddress(sender);
        streamManager.registerUdpSession(sessionKey, streamContext.getStreamId());
        streamManager.scheduleUdpSessionTimeout(sessionKey, channel.eventLoop(), SESSION_IDLE_TIMEOUT_SECONDS);

        streamContext.setPendingFirstPacket(packet.content().retain());
        streamContext.fireEvent(StreamEvent.STREAM_OPEN);
    }

    private void handleDatagram(StreamContext streamContext, ByteBuf content) {
        TunnelEntry tunnelEntry = streamContext.getTunnelEntry();
        if (tunnelEntry == null) {
            logger.error("[UDP] 隧道连接不存在，关闭流 {}", streamContext.getStreamId());
            streamContext.fireEvent(StreamEvent.STREAM_LOCAL_CLOSE);
            return;
        }
        Channel tunnel = tunnelEntry.getChannel();
        if (!tunnel.isWritable()) {
            logger.debug("[UDP] 隧道不可写，暂停处理 streamId={}", streamContext.getStreamId());
            if (tunnelEntry.getTunnelType().isMultiplex()) {
                streamManager.addPausedStreamId(tunnel, streamContext.getStreamId());
            }
            return;
        }
        streamContext.forwardToLocal(content);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        logger.error("[UDP] 代理通道异常", cause);
    }
}
