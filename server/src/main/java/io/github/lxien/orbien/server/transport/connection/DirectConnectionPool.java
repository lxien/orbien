package io.github.lxien.orbien.server.transport.connection;

import io.github.lxien.orbien.core.transport.TunnelEntry;
import io.github.lxien.orbien.core.utils.ChannelUtils;
import io.netty.channel.Channel;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 独立隧道连接池
 */
@Component
public class DirectConnectionPool {

    /**
     * 最大回收总连接数限制
     */
    private static final int MAX_TOTAL_CONNECTIONS = 100;

    /**
     * 总连接数计数器
     */
    private final AtomicInteger totalConnections = new AtomicInteger(0);

    /**
     * 明文连接数计数器
     */
    private final AtomicInteger plainConnections = new AtomicInteger(0);

    /**
     * 加密连接数计数器
     */
    private final AtomicInteger encryptConnections = new AtomicInteger(0);

    /**
     * agentId --> Pool
     */
    private final ConcurrentHashMap<String, Pool> agentPools = new ConcurrentHashMap<>();

    public TunnelEntry borrow(String agentId, String tunnelId, boolean isEncrypt) {
        Pool pool = agentPools.get(agentId);
        if (pool == null) {
            return null;
        }
        TunnelEntry entry = isEncrypt ? pool.borrowEncrypt(tunnelId) : pool.borrowPlain(tunnelId);
        if (entry != null) {
            totalConnections.decrementAndGet();
            if (isEncrypt) {
                encryptConnections.decrementAndGet();
            } else {
                plainConnections.decrementAndGet();
            }
        }
        return entry;
    }

    public void release(String agentId, TunnelEntry tunnelEntry) {
        if (tunnelEntry == null || tunnelEntry.getTunnelId() == null) {
            return;
        }
        Pool pool = agentPools.get(agentId);
        if (pool == null) {
            return;
        }
        pool.release(tunnelEntry.getTunnelId(), tunnelEntry);
    }

    public void remove(String agentId, String tunnelId) {
        Pool pool = agentPools.get(agentId);
        if (pool != null) {
            pool.remove(tunnelId);
        }
    }

    /**
     * 按 channel 移除独立隧道（解码异常、连接污染等场景）。
     */
    public boolean removeByChannel(Channel channel) {
        if (channel == null) {
            return false;
        }
        for (Map.Entry<String, Pool> entry : agentPools.entrySet()) {
            if (entry.getValue().removeByChannel(channel)) {
                return true;
            }
        }
        return false;
    }

    public boolean register(String agentId, TunnelEntry tunnelEntry) {
        if (agentId == null || tunnelEntry == null || tunnelEntry.getTunnelId() == null) {
            return false;
        }
        totalConnections.incrementAndGet();
        if (tunnelEntry.isEncrypt()) {
            encryptConnections.incrementAndGet();
        } else {
            plainConnections.incrementAndGet();
        }

        Pool pool = agentPools.computeIfAbsent(agentId, k -> new Pool());
        pool.register(tunnelEntry.getTunnelId(), tunnelEntry);
        return true;
    }

    public void offline(String agentId) {
        Pool pool = agentPools.remove(agentId);
        if (pool != null) {
            int plainCount = pool.plainPools.size();
            int encryptCount = pool.encryptPools.size();

            totalConnections.addAndGet(-(plainCount + encryptCount));
            plainConnections.addAndGet(-plainCount);
            encryptConnections.addAndGet(-encryptCount);

            pool.offline();
        }
    }

    /**
     * 获取总连接数
     */
    public int getTotalConnections() {
        return totalConnections.get();
    }

    /**
     * 获取明文连接数
     */
    public int getPlainConnections() {
        return plainConnections.get();
    }

    /**
     * 获取加密连接数
     */
    public int getEncryptConnections() {
        return encryptConnections.get();
    }

    /**
     * 获取最大总连接数
     */
    public int getMaxTotalConnections() {
        return MAX_TOTAL_CONNECTIONS;
    }

    /**
     * 判断连接数是否超过限制
     */
    public boolean isConnectionLimitReached() {
        return totalConnections.get() >= MAX_TOTAL_CONNECTIONS;
    }

    class Pool {
        private final Map<String, TunnelEntry> plainPools = new ConcurrentHashMap<>();
        private final Map<String, TunnelEntry> encryptPools = new ConcurrentHashMap<>();

        public TunnelEntry borrowPlain(String tunnelId) {
            return plainPools.remove(tunnelId);
        }

        public TunnelEntry borrowEncrypt(String tunnelId) {
            return encryptPools.remove(tunnelId);
        }

        public void register(String tunnelId, TunnelEntry tunnelEntry) {
            if (tunnelEntry.isEncrypt()) {
                encryptPools.putIfAbsent(tunnelId, tunnelEntry);
            } else {
                plainPools.putIfAbsent(tunnelId, tunnelEntry);
            }

        }

        public void registerPlain(String tunnelId, TunnelEntry channel) {
            if (plainPools.putIfAbsent(tunnelId, channel) == null) {
                totalConnections.incrementAndGet();
                plainConnections.incrementAndGet();
            }
        }

        public void registerEncrypt(String tunnelId, TunnelEntry channel) {
            if (encryptPools.putIfAbsent(tunnelId, channel) == null) {
                totalConnections.incrementAndGet();
                encryptConnections.incrementAndGet();
            }
        }

        public void remove(String tunnelId) {
            if (plainPools.remove(tunnelId) != null) {
                totalConnections.decrementAndGet();
                plainConnections.decrementAndGet();
            }
            if (encryptPools.remove(tunnelId) != null) {
                totalConnections.decrementAndGet();
                encryptConnections.decrementAndGet();
            }
        }

        boolean removeByChannel(Channel channel) {
            boolean removed = false;
            removed |= removeMatching(plainPools, channel, false);
            removed |= removeMatching(encryptPools, channel, true);
            return removed;
        }

        private boolean removeMatching(Map<String, TunnelEntry> pools, Channel channel, boolean encrypt) {
            String matchedId = null;
            for (Map.Entry<String, TunnelEntry> entry : pools.entrySet()) {
                TunnelEntry tunnelEntry = entry.getValue();
                if (tunnelEntry != null && tunnelEntry.getChannel() == channel) {
                    matchedId = entry.getKey();
                    break;
                }
            }
            if (matchedId == null) {
                return false;
            }
            pools.remove(matchedId);
            totalConnections.decrementAndGet();
            if (encrypt) {
                encryptConnections.decrementAndGet();
            } else {
                plainConnections.decrementAndGet();
            }
            return true;
        }

        public void release(String tunnelId, TunnelEntry tunnelEntry) {
            if (tunnelEntry == null) {
                return;
            }
            Channel channel = tunnelEntry.getChannel();
            channel.config().setAutoRead(true);
            if (tunnelEntry.isEncrypt()) {
                encryptPools.putIfAbsent(tunnelId, tunnelEntry);
            } else {
                plainPools.putIfAbsent(tunnelId, tunnelEntry);
            }
        }

        public void offline() {
            plainPools.values().forEach(ch -> {
                if (ch != null && ch.getChannel().isActive()) {
                    ChannelUtils.closeOnFlush(ch.getChannel());
                }
            });
            encryptPools.values().forEach(ch -> {
                if (ch != null && ch.getChannel().isActive()) {
                    ChannelUtils.closeOnFlush(ch.getChannel());
                }
            });
            plainPools.clear();
            encryptPools.clear();
        }
    }

}
