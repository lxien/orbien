package io.github.lxien.orbien.server.transport.connection;

import io.github.lxien.orbien.core.transport.TunnelEntry;
import io.github.lxien.orbien.core.utils.ChannelUtils;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

@Component
public class MultiplexConnectionPool {
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(MultiplexConnectionPool.class);

    /**
     * agentId --> Pool
     * 存储每个客户端的连接池
     */
    private final ConcurrentHashMap<String, Pool> agentPools = new ConcurrentHashMap<>();


    public TunnelEntry acquire(String agentId, boolean isTls) {
        Pool pool = agentPools.get(agentId);
        if (pool == null) {
            logger.warn("客户端 {} 连接池为空，没有可用连接", agentId);
            return null;
        }
        return pool.acquire(isTls);
    }

    public void setChannel(String agentId, boolean isTls, TunnelEntry entry) {
        if (agentId == null || entry == null) {
            throw new IllegalArgumentException("客户端 ID或隧道节点不能为空");
        }
        Pool pool = agentPools.computeIfAbsent(agentId, k -> new Pool());
        pool.setChannel(isTls, entry);
    }

    public void offline(String agentId) {
        if (agentId == null) {
            return;
        }
        Pool pool = agentPools.remove(agentId);
        if (pool != null) {
            pool.offline();
        }
    }

    static class Pool {
        protected TunnelEntry plainEntry;
        protected TunnelEntry tlsEntry;

        public TunnelEntry acquire(boolean isTls) {
            return isTls ? tlsEntry : plainEntry;
        }

        public void setChannel(boolean isTls, TunnelEntry entry) {
            if (isTls) {
                this.tlsEntry = entry;
            } else {
                this.plainEntry = entry;
            }
        }

        public void offline() {
            if (plainEntry != null) {
                ChannelUtils.closeOnFlush(plainEntry.getChannel());
            }
            if (tlsEntry != null) {
                ChannelUtils.closeOnFlush(tlsEntry.getChannel());
            }
        }
    }

}
