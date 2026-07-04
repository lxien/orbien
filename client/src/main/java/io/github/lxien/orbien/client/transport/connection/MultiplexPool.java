package io.github.lxien.orbien.client.transport.connection;

import io.github.lxien.orbien.client.common.UUIDGenerator;
import io.github.lxien.orbien.core.enums.TunnelType;
import io.github.lxien.orbien.core.transport.TunnelEntry;
import io.github.lxien.orbien.core.utils.ChannelUtils;
import io.netty.channel.Channel;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;


public class MultiplexPool {
    private final InternalLogger logger = InternalLoggerFactory.getInstance(MultiplexPool.class);
    private TunnelEntry tlsTunnelEntry;
    private TunnelEntry plainTunnelEntry;

    public TunnelEntry acquire(boolean isTls) {
        TunnelEntry tunnelEntry = isTls ? tlsTunnelEntry : plainTunnelEntry;
        if (tunnelEntry == null) {
            return null;
        }
        Channel channel = tunnelEntry.getChannel();
        if (channel.isActive()) {
            return tunnelEntry;
        }
        logger.warn("多路复用连接已失效");
        clearTunnel(isTls);
        return null;
    }

    public TunnelEntry createChannel(boolean isTls, Channel tunnel) {
        if (!tunnel.isActive()) {
            return null;
        }
        String tunnelId = UUIDGenerator.generate();
        //NettyBatchWriteQueue writeQueue = NettyBatchWriteQueue.createWriteQueue(tunnel);
        TunnelEntry tunnelEntry = new TunnelEntry(tunnelId, isTls, tunnel, TunnelType.MULTIPLEX);
        if (isTls) {
            this.tlsTunnelEntry = tunnelEntry;
        } else {
            this.plainTunnelEntry = tunnelEntry;
        }
        return tunnelEntry;
    }

    public TunnelEntry activeTunnel(boolean isTls) {
        if (isTls) {
            tlsTunnelEntry.setActive(true);
            return tlsTunnelEntry;
        } else {
            plainTunnelEntry.setActive(true);
            return plainTunnelEntry;
        }
    }

    public void clearTunnel(boolean isTls) {
        logger.debug("清空多路复用连接");
        if (isTls && tlsTunnelEntry != null) {
            ChannelUtils.closeOnFlush(tlsTunnelEntry.getChannel());
            this.tlsTunnelEntry = null;
            return;
        }
        if (this.plainTunnelEntry != null) {
            ChannelUtils.closeOnFlush(plainTunnelEntry.getChannel());
            this.plainTunnelEntry = null;
        }
    }

    public void closeAll() {
        if (tlsTunnelEntry != null) {
            ChannelUtils.closeOnFlush(tlsTunnelEntry.getChannel());
        }
        if (plainTunnelEntry != null) {
            ChannelUtils.closeOnFlush(plainTunnelEntry.getChannel());
        }
    }
}
