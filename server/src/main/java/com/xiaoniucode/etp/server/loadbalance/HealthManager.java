package com.xiaoniucode.etp.server.loadbalance;

import com.xiaoniucode.etp.core.domain.Target;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 代理内网服务健康状态管理器
 */
@Component
public class HealthManager {
    private final ConcurrentHashMap<String/*proxyId*/, ConcurrentHashMap<String/*host:port*/, Boolean>> healthStore = new ConcurrentHashMap<>();

    public void updateHealth(String proxyId, Target target, boolean healthy) {
        if (proxyId == null || target == null) return;

        ConcurrentHashMap<String, Boolean> targetHealth = healthStore.computeIfAbsent(proxyId,
                k -> new ConcurrentHashMap<>()
        );
        String key = buildTargetKey(target);
        targetHealth.put(key, healthy);
    }

    public void batchUpdate(String proxyId, Map<Target, Boolean> statusMap) {
        if (statusMap == null || statusMap.isEmpty()) return;

        ConcurrentHashMap<String, Boolean> targetHealth = healthStore.computeIfAbsent(proxyId,
                k -> new ConcurrentHashMap<>());

        statusMap.forEach((target, healthy) -> {
            String key = buildTargetKey(target);
            targetHealth.put(key, healthy);
        });
    }

    public List<Target> getAvailableTargets(String proxyId, List<Target> targets) {
        if (targets == null || targets.isEmpty()) {
            return List.of();
        }

        ConcurrentHashMap<String, Boolean> targetHealth = healthStore.get(proxyId);
        if (targetHealth == null) {
            return targets;
        }

        return targets.stream().filter(target -> {
            String key = buildTargetKey(target);
            return targetHealth.getOrDefault(key, true);
        }).collect(Collectors.toList());
    }

    public void removeProxy(String proxyId) {
        if (proxyId != null) {
            healthStore.remove(proxyId);
        }
    }

    public void removeTarget(String proxyId, Target target) {
        if (proxyId == null || target == null) return;

        String key = buildTargetKey(target);
        healthStore.computeIfPresent(proxyId, (pid, targetHealth) -> {
            targetHealth.remove(key);
            if (targetHealth.isEmpty()) {
                return null;
            }
            return targetHealth;
        });
    }

    public void clear() {
        healthStore.clear();
    }

    private String buildTargetKey(Target target) {
        return target.getHost() + ":" + target.getPort();
    }
}