package io.github.lxien.orbien.client.transport.connection;

import io.github.lxien.orbien.core.transport.TunnelEntry;
import io.github.lxien.orbien.core.transport.api.TransportPoolKey;
import io.github.lxien.orbien.core.enums.TransportProtocol;
import io.netty.channel.Channel;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TransportPoolManager {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(TransportPoolManager.class);

    private final Map<TransportPoolKey, ProtocolMultiplexPool> multiplexPools = new ConcurrentHashMap<>();
    private final Map<TransportPoolKey, ProtocolDirectPool> directPools = new ConcurrentHashMap<>();

    public TunnelEntry acquire(TransportProtocol protocol, boolean encrypt, boolean multiplex) {
        TransportPoolKey key = multiplex
                ? TransportPoolKey.multiplex(protocol, encrypt)
                : TransportPoolKey.direct(protocol, encrypt);
        logger.debug("[传输] 从连接池获取隧道 protocol={} encrypt={} multiplex={}",
                protocol.getName(), encrypt, multiplex);
        if (multiplex) {
            return multiplexPools.computeIfAbsent(key, ProtocolMultiplexPool::new).acquire(encrypt);
        }
        return directPools.computeIfAbsent(key, ProtocolDirectPool::new).borrow(encrypt);
    }

    public TunnelEntry createMultiplex(TransportProtocol protocol, boolean encrypt, Channel channel) {
        TransportPoolKey key = TransportPoolKey.multiplex(protocol, encrypt);
        return multiplexPools.computeIfAbsent(key, ProtocolMultiplexPool::new).createChannel(encrypt, channel);
    }

    public TunnelEntry createDirect(TransportProtocol protocol, boolean encrypt, Channel channel) {
        TransportPoolKey key = TransportPoolKey.direct(protocol, encrypt);
        return directPools.computeIfAbsent(key, ProtocolDirectPool::new).createTunnel(channel, encrypt);
    }

    public TunnelEntry activateMultiplex(TransportProtocol protocol, boolean encrypt) {
        TransportPoolKey key = TransportPoolKey.multiplex(protocol, encrypt);
        ProtocolMultiplexPool pool = multiplexPools.get(key);
        return pool != null ? pool.activeTunnel(encrypt) : null;
    }

    public TunnelEntry activateDirect(TransportProtocol protocol, String tunnelId) {
        for (ProtocolDirectPool pool : directPools.values()) {
            TunnelEntry entry = pool.activateTunnel(tunnelId);
            if (entry != null) {
                return entry;
            }
        }
        return null;
    }

    public TransportProtocol findProtocolByTunnelId(String tunnelId) {
        if (tunnelId == null || tunnelId.isBlank()) {
            return null;
        }
        for (ProtocolMultiplexPool pool : multiplexPools.values()) {
            TunnelEntry entry = pool.findByTunnelId(tunnelId);
            if (entry != null) {
                return entry.getProtocol();
            }
        }
        for (ProtocolDirectPool pool : directPools.values()) {
            TunnelEntry entry = pool.findByTunnelId(tunnelId);
            if (entry != null) {
                return entry.getProtocol();
            }
        }
        return null;
    }

    public void clearMultiplex(TransportProtocol protocol, boolean encrypt) {
        TransportPoolKey key = TransportPoolKey.multiplex(protocol, encrypt);
        ProtocolMultiplexPool pool = multiplexPools.get(key);
        if (pool != null) {
            pool.clearTunnel(encrypt);
        }
    }

    public void removeDirect(String tunnelId) {
        for (ProtocolDirectPool pool : directPools.values()) {
            pool.removeTunnel(tunnelId);
        }
    }

    public void releaseDirect(TunnelEntry entry) {
        if (entry == null) {
            return;
        }
        TransportPoolKey key = TransportPoolKey.direct(entry.getProtocol(), entry.isEncrypt());
        ProtocolDirectPool pool = directPools.get(key);
        if (pool != null) {
            pool.release(entry);
        }
    }

    public void closeAll() {
        multiplexPools.values().forEach(ProtocolMultiplexPool::closeAll);
        directPools.values().forEach(ProtocolDirectPool::closeAll);
    }
}
