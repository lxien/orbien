package com.xiaoniucode.etp.server.loadbalance;

import com.xiaoniucode.etp.core.domain.Target;
import com.xiaoniucode.etp.core.message.Message;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 代理内网服务健康状态管理器
 */
@Component
public class HealthManager {
    private final ConcurrentHashMap<String/*proxyId*/, ConcurrentHashMap<String/*host:port*/, Message.HealthStatus>> healthStore = new ConcurrentHashMap<>();

    public void updateHealth(String proxyId, String host, Integer port, Message.HealthStatus status) {
        if (proxyId == null || host == null || port == null) return;

        ConcurrentHashMap<String, Message.HealthStatus> targetHealth = healthStore.computeIfAbsent(proxyId,
                k -> new ConcurrentHashMap<>()
        );
        String key = buildTargetKey(host, port);
        targetHealth.put(key, status);
    }

    public List<Target> getAvailableTargets(String proxyId, List<Target> targets) {
        if (targets == null || targets.isEmpty()) {
            return List.of();
        }

        ConcurrentHashMap<String, Message.HealthStatus> targetHealth = healthStore.get(proxyId);
        if (targetHealth == null) {
            return targets;
        }

        return targets.stream().filter(target -> {
            String key = buildTargetKey(target.getHost(),target.getPort());
            Message.HealthStatus status = targetHealth.get(key);
            return status == null || status == Message.HealthStatus.UP;
        }).collect(Collectors.toList());
    }

    public void removeProxy(String proxyId) {
        if (proxyId != null) {
            healthStore.remove(proxyId);
        }
    }

    public void removeTarget(String proxyId, String host, Integer port) {
        if (proxyId == null || host == null || port == null) return;

        String key = buildTargetKey(host, port);
        healthStore.computeIfPresent(proxyId, (pid, targetHealth) -> {
            targetHealth.remove(key);
            if (targetHealth.isEmpty()) {
                return null;
            }
            return targetHealth;
        });
    }

    private String buildTargetKey(String host, Integer port) {
        return host + ":" + port;
    }
}