package io.github.lxien.orbien.server.loadbalance;


import io.github.lxien.orbien.core.domain.Target;

import java.util.List;

/**
 * 负载均衡器接口
 */
public interface LoadBalancer {

    /**
     * 选择一个目标
     *
     * @param targets 可用的目标列表
     * @return 选中的目标，如果没有可用返回null
     */
    Target select(String proxyId,List<Target> targets);
}