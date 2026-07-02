package com.xiaoniucode.etp.server.port;

import com.xiaoniucode.etp.core.domain.PortInterval;
import com.xiaoniucode.etp.core.enums.PortPoolType;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 按协议隔离的端口池门面，支持TCP和UDP协议端口资源管理
 */
@Component
public class PortPoolManager {
    private final Map<PortPoolType, PortPool> pools = new EnumMap<>(PortPoolType.class);

    public PortPoolManager() {
        for (PortPoolType type : PortPoolType.values()) {
            pools.put(type, new PortPool(type));
        }
    }

    public Integer acquire(PortPoolType type) {
        return pool(type).acquire();
    }

    public void reserve(PortPoolType type, int port) {
        pool(type).reserve(port);
    }

    public boolean release(PortPoolType type, int port) {
        return pool(type).release(port);
    }

    public boolean isAvailable(PortPoolType type, int port) {
        return pool(type).isAvailable(port);
    }

    public List<Integer> suggestAvailable(PortPoolType type, int limit) {
        return pool(type).suggestAvailable(limit);
    }

    public void markAllocated(PortPoolType type, int port) {
        pool(type).markAllocated(port);
    }

    public void replaceAllowed(PortPoolType type, Collection<PortInterval> intervals) {
        pool(type).replaceAllowed(intervals);
    }

    public void addAllowed(PortPoolType type, PortInterval interval) {
        pool(type).addAllowed(interval);
    }

    public void removeAllowed(PortPoolType type, PortInterval interval) {
        pool(type).removeAllowed(interval);
    }

    public void validateRemovable(PortPoolType type, PortInterval interval) {
        pool(type).validateRemovable(interval);
    }

    private PortPool pool(PortPoolType type) {
        PortPool pool = pools.get(type);
        if (pool == null) {
            throw new IllegalArgumentException("未知端口池类型: " + type);
        }
        return pool;
    }
}
