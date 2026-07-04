package io.github.lxien.orbien.client.transport.bridge;

import io.github.lxien.orbien.client.statemachine.stream.StreamContext;
import io.github.lxien.orbien.client.statemachine.stream.StreamEvent;
import io.github.lxien.orbien.core.message.TMSP;
import io.github.lxien.orbien.core.message.TMSPFrame;
import io.github.lxien.orbien.core.transport.TunnelBridge;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.socket.DatagramPacket;
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
    public void open() {
    }

    @Override
    public void forwardToLocal(ByteBuf payload) {
        int streamId = streamContext.getStreamId();
        if (!server.isActive()) {
            logger.debug("通道未激活，数据转发到内网失败，关闭流：streamId={}", streamId);
            streamContext.fireEvent(StreamEvent.STREAM_LOCAL_CLOSE);
            return;
        }
        if (streamContext.isDatagram()) {
            InetSocketAddress target = new InetSocketAddress(streamContext.getLocalIp(), streamContext.getLocalPort());
            DatagramPacket packet = new DatagramPacket(payload.retain(), target);
            server.writeAndFlush(packet).addListener((ChannelFutureListener) future -> {
                if (!future.isSuccess()) {
                    logger.warn("UDP 数据转发到内网失败，关闭流：streamId={}", streamId, future.cause());
                }
            });
            return;
        }
        server.writeAndFlush(payload.retain()).addListener((ChannelFutureListener) future -> {
            logger.debug("流 {} 引用计数为：{}", streamContext.getStreamId(), payload.refCnt());
            if (!future.isSuccess()) {
                logger.warn("数据转发到内网真实服务失败，关闭流：streamId={}", streamId, future.cause());
            } else {
                logger.debug("数据转发到内网真实服务成功：streamId={}", streamId);
            }
        });
    }

    @Override
    public void forwardToRemote(ByteBuf payload) {
        if (!tunnel.isActive()) {
            logger.debug("通道未激活，数据转发到远程失败，关闭流：streamId={}", streamContext.getStreamId());
            streamContext.fireEvent(StreamEvent.STREAM_LOCAL_CLOSE);
            return;
        }
        TMSPFrame frame = new TMSPFrame(streamContext.getStreamId(), TMSP.MSG_STREAM_DATA, payload);
        tunnel.writeAndFlush(frame.retain()).addListener((ChannelFutureListener) future -> {
            logger.debug("流 {} 引用计数为：{}", streamContext.getStreamId(), payload.refCnt());
            int streamId = streamContext.getStreamId();
            if (!future.isSuccess()) {
                logger.warn("数据转发到远程隧道失败: streamId={}", streamId, future.cause());
            } else {
                logger.debug("数据转发到远程隧道成功: streamId={}", streamId);
            }
        });
    }
}