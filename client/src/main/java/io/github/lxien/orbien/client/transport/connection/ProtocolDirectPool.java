package io.github.lxien.orbien.client.transport.connection;

import io.github.lxien.orbien.client.common.UUIDGenerator;
import io.github.lxien.orbien.core.enums.TunnelType;
import io.github.lxien.orbien.core.transport.TunnelEntry;
import io.github.lxien.orbien.core.transport.api.TransportPoolKey;
import io.github.lxien.orbien.core.utils.ChannelUtils;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ProtocolDirectPool {

    private static final int MAX_TUNNEL_POOL_SIZE = 100;
    private final InternalLogger logger = InternalLoggerFactory.getInstance(ProtocolDirectPool.class);
    private final TransportPoolKey poolKey;
    private final Map<String, TunnelEntry> plainTunnels = new ConcurrentHashMap<>(5);
    private final Map<String, TunnelEntry> encryptTunnels = new ConcurrentHashMap<>(5);

    public ProtocolDirectPool(TransportPoolKey poolKey) {
        this.poolKey = poolKey;
    }

    public TunnelEntry borrow(boolean isEncrypt) {
        Map<String, TunnelEntry> tunnels = isEncrypt ? encryptTunnels : plainTunnels;
        for (Map.Entry<String, TunnelEntry> mapEntry : tunnels.entrySet()) {
            TunnelEntry entry = mapEntry.getValue();
            if (entry.isActive()) {
                return tunnels.remove(mapEntry.getKey());
            }
            removeTunnel(entry.getTunnelId());
        }
        logger.debug("池中没有可用的活跃{}隧道: {}", isEncrypt ? "加密" : "明文", poolKey);
        return null;
    }

    public TunnelEntry createTunnel(Channel channel, boolean isEncrypt) {
        if (plainTunnels.size() + encryptTunnels.size() >= MAX_TUNNEL_POOL_SIZE) {
            return null;
        }
        String tunnelId = UUIDGenerator.generate();
        TunnelEntry tunnelEntry = new TunnelEntry(
                tunnelId, poolKey.protocol(), isEncrypt, channel, TunnelType.DIRECT);
        if (isEncrypt) {
            encryptTunnels.putIfAbsent(tunnelId, tunnelEntry);
        } else {
            plainTunnels.putIfAbsent(tunnelId, tunnelEntry);
        }
        return tunnelEntry;
    }

    public TunnelEntry activateTunnel(String tunnelId) {
        TunnelEntry entry = plainTunnels.get(tunnelId);
        if (entry == null) {
            entry = encryptTunnels.get(tunnelId);
        }
        if (entry != null) {
            entry.setActive(true);
        }
        return entry;
    }

    public TunnelEntry findByTunnelId(String tunnelId) {
        if (tunnelId == null) {
            return null;
        }
        TunnelEntry entry = plainTunnels.get(tunnelId);
        return entry != null ? entry : encryptTunnels.get(tunnelId);
    }

    public void release(TunnelEntry entry) {
        if (entry == null) {
            return;
        }
        Channel tunnel = entry.getChannel();
        if (plainTunnels.size() + encryptTunnels.size() > MAX_TUNNEL_POOL_SIZE) {
            tunnel.close();
            removeTunnel(entry.getTunnelId());
        } else {
            tunnel.config().setOption(ChannelOption.AUTO_READ, true);
            if (entry.isEncrypt()) {
                encryptTunnels.putIfAbsent(entry.getTunnelId(), entry);
            } else {
                plainTunnels.putIfAbsent(entry.getTunnelId(), entry);
            }
        }
    }

    public void removeTunnel(String tunnelId) {
        TunnelEntry entry = plainTunnels.remove(tunnelId);
        if (entry == null) {
            entry = encryptTunnels.remove(tunnelId);
        }
        if (entry != null && entry.getChannel() != null && entry.getChannel().isActive()) {
            entry.getChannel().close();
        }
    }

    public void closeAll() {
        plainTunnels.values().forEach(entry -> ChannelUtils.closeOnFlush(entry.getChannel()));
        encryptTunnels.values().forEach(entry -> ChannelUtils.closeOnFlush(entry.getChannel()));
    }
}
