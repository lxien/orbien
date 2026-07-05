package io.github.lxien.orbien.server.transport.connection;

import io.github.lxien.orbien.core.transport.TunnelEntry;
import io.github.lxien.orbien.core.transport.api.TransportPoolKey;
import io.github.lxien.orbien.core.enums.TransportProtocol;
import io.github.lxien.orbien.core.utils.ChannelUtils;
import io.netty.channel.Channel;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class MultiplexConnectionPool {
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(MultiplexConnectionPool.class);

    private final ConcurrentHashMap<String, AgentPool> agentPools = new ConcurrentHashMap<>();

    public TunnelEntry acquire(String agentId, TransportProtocol protocol, boolean isTls) {
        AgentPool pool = agentPools.get(agentId);
        if (pool == null) {
            logger.warn("[传输] 客户端 {} 连接池为空 protocol={} encrypt={}", agentId, protocol.getName(), isTls);
            return null;
        }
        TunnelEntry entry = pool.acquire(protocol, isTls);
        if (entry == null) {
            logger.warn("[传输] 客户端 {} 无可用多路复用隧道 protocol={} encrypt={}",
                    agentId, protocol.getName(), isTls);
        } else {
            logger.debug("[传输] 客户端 {} 命中多路复用隧道 protocol={} encrypt={} tunnelId={} channelClass={} active={}",
                    agentId, protocol.getName(), isTls, entry.getTunnelId(),
                    entry.getChannel().getClass().getSimpleName(), entry.getChannel().isActive());
        }
        return entry;
    }

    public void setChannel(String agentId, TransportProtocol protocol, boolean isTls, TunnelEntry entry) {
        if (agentId == null || entry == null) {
            throw new IllegalArgumentException("客户端 ID或隧道节点不能为空");
        }
        logger.debug("[传输] 客户端 {} 注册多路复用隧道 protocol={} encrypt={} tunnelId={} channelClass={}",
                agentId, protocol.getName(), isTls, entry.getTunnelId(),
                entry.getChannel().getClass().getSimpleName());
        AgentPool pool = agentPools.computeIfAbsent(agentId, k -> new AgentPool());
        pool.setChannel(protocol, isTls, entry);
    }

    public void offline(String agentId) {
        if (agentId == null) {
            return;
        }
        AgentPool pool = agentPools.remove(agentId);
        if (pool != null) {
            pool.offline();
        }
    }

    /**
     * 数据隧道断开时从池中移除，避免后续 acquire 命中已关闭 channel。
     */
    public void removeByChannel(Channel channel) {
        if (channel == null) {
            return;
        }
        agentPools.forEach((agentId, pool) -> {
            if (pool.removeByChannel(channel)) {
                logger.warn("[传输] 客户端 {} 数据隧道已断开并从连接池移除 channelClass={}",
                        agentId, channel.getClass().getSimpleName());
            }
        });
    }

    static class AgentPool {
        private final Map<TransportPoolKey, TunnelEntry> entries = new ConcurrentHashMap<>();

        public TunnelEntry acquire(TransportProtocol protocol, boolean isTls) {
            return entries.get(TransportPoolKey.multiplex(protocol, isTls));
        }

        public void setChannel(TransportProtocol protocol, boolean isTls, TunnelEntry entry) {
            entries.put(TransportPoolKey.multiplex(protocol, isTls), entry);
        }

        public void offline() {
            entries.values().forEach(entry -> ChannelUtils.closeOnFlush(entry.getChannel()));
            entries.clear();
        }

        boolean removeByChannel(Channel channel) {
            return entries.entrySet().removeIf(e -> {
                TunnelEntry entry = e.getValue();
                return entry != null && entry.getChannel() == channel;
            });
        }
    }
}
