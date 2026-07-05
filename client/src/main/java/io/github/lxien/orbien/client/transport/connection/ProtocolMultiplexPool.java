package io.github.lxien.orbien.client.transport.connection;

import io.github.lxien.orbien.client.common.UUIDGenerator;
import io.github.lxien.orbien.core.enums.TunnelType;
import io.github.lxien.orbien.core.transport.TunnelEntry;
import io.github.lxien.orbien.core.transport.api.TransportPoolKey;
import io.github.lxien.orbien.core.utils.ChannelUtils;
import io.netty.channel.Channel;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

public class ProtocolMultiplexPool {

    private final InternalLogger logger = InternalLoggerFactory.getInstance(ProtocolMultiplexPool.class);
    private final TransportPoolKey poolKey;
    private TunnelEntry tlsTunnelEntry;
    private TunnelEntry plainTunnelEntry;

    public ProtocolMultiplexPool(TransportPoolKey poolKey) {
        this.poolKey = poolKey;
    }

    public TunnelEntry acquire(boolean isTls) {
        TunnelEntry tunnelEntry = isTls ? tlsTunnelEntry : plainTunnelEntry;
        if (tunnelEntry == null) {
            return null;
        }
        if (tunnelEntry.getChannel().isActive()) {
            return tunnelEntry;
        }
        logger.warn("多路复用连接已失效: {}", poolKey);
        clearTunnel(isTls);
        return null;
    }

    public TunnelEntry createChannel(boolean isTls, Channel tunnel) {
        if (!tunnel.isActive()) {
            return null;
        }
        String tunnelId = UUIDGenerator.generate();
        TunnelEntry tunnelEntry = new TunnelEntry(
                tunnelId, poolKey.protocol(), isTls, tunnel, TunnelType.MULTIPLEX);
        if (isTls) {
            this.tlsTunnelEntry = tunnelEntry;
        } else {
            this.plainTunnelEntry = tunnelEntry;
        }
        return tunnelEntry;
    }

    public TunnelEntry activeTunnel(boolean isTls) {
        TunnelEntry entry = isTls ? tlsTunnelEntry : plainTunnelEntry;
        if (entry != null) {
            entry.setActive(true);
        }
        return entry;
    }

    public TunnelEntry findByTunnelId(String tunnelId) {
        if (tunnelId == null) {
            return null;
        }
        if (tlsTunnelEntry != null && tunnelId.equals(tlsTunnelEntry.getTunnelId())) {
            return tlsTunnelEntry;
        }
        if (plainTunnelEntry != null && tunnelId.equals(plainTunnelEntry.getTunnelId())) {
            return plainTunnelEntry;
        }
        return null;
    }

    public void clearTunnel(boolean isTls) {
        if (isTls && tlsTunnelEntry != null) {
            ChannelUtils.closeOnFlush(tlsTunnelEntry.getChannel());
            this.tlsTunnelEntry = null;
            return;
        }
        if (plainTunnelEntry != null) {
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
