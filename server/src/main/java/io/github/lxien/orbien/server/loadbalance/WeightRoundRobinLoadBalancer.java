package io.github.lxien.orbien.server.loadbalance;

import io.github.lxien.orbien.core.domain.Target;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 加权轮询负载均衡器，支持动态权重调整和节点自动回收
 */
@Component
public class WeightRoundRobinLoadBalancer implements LoadBalancer {
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(WeightRoundRobinLoadBalancer.class);

    /**
     * 节点回收周期，单位：毫秒
     */
    private static final int RECYCLE_PERIOD = 60000;

    /**
     * 加权轮询数据结构
     * 存储每个目标服务器的权重和当前状态
     */
    @Data
    protected static class WeightedRoundRobin {
        /**
         * 目标服务器权重
         * 获取目标服务器权重
         *
         */
        private int weight;

        /**
         * 当前权重值，用于轮询算法计算
         */
        private final AtomicLong current = new AtomicLong(0);

        /**
         * 最后更新时间，用于判断是否需要回收
         */
        private long lastUpdate;

        /**
         * 设置目标服务器权重
         *
         * @param weight 权重值
         */
        public void setWeight(int weight) {
            this.weight = weight;
            // 权重变更时重置当前权重
            current.set(0);
        }

        /**
         * 增加当前权重
         *
         * @return 增加后的权重值
         */
        public long increaseCurrent() {
            return current.addAndGet(weight);
        }

        /**
         * 选择后更新权重
         *
         * @param total 总权重
         */
        public void sel(int total) {
            current.addAndGet(-1 * total);
        }

    }

    /**
     * 存储代理的权重信息，key为代理ID，value为该代理下所有目标服务器的权重信息
     */
    private final ConcurrentMap<String, ConcurrentMap<String, WeightedRoundRobin>> proxyWeightMap = new ConcurrentHashMap<>();

    /**
     * 构造方法
     */
    public WeightRoundRobinLoadBalancer() {
        logger.debug("创建加权轮询负载均衡器");
    }

    /**
     * 选择目标服务器
     *
     * @param proxyId 代理ID
     * @param targets 目标服务器列表
     * @return 选中的目标服务器
     */
    @Override
    public Target select(String proxyId, List<Target> targets) {
        if (targets == null || targets.isEmpty()) {
            logger.warn("目标服务器列表为空");
            return null;
        }

        // 获取当前代理的权重映射，如果不存在则创建
        ConcurrentMap<String, WeightedRoundRobin> map = proxyWeightMap.computeIfAbsent(proxyId, k -> new ConcurrentHashMap<>());
        int totalWeight = 0;
        long maxCurrent = Long.MIN_VALUE;
        long now = System.currentTimeMillis();
        Target selectedTarget = null;
        WeightedRoundRobin selectedWRR = null;

        // 遍历所有目标服务器，计算权重并选择
        for (Target target : targets) {
            // 使用host:port作为目标服务器的唯一标识
            String identifyString = target.getHost() + ":" + target.getPort();
            int weight = target.getWeight();

            // 获取或创建目标服务器的权重信息
            WeightedRoundRobin weightedRoundRobin = map.computeIfAbsent(identifyString, k -> {
                WeightedRoundRobin wrr = new WeightedRoundRobin();
                wrr.setWeight(weight);
                return wrr;
            });

            // 如果权重发生变化，更新权重
            if (weight != weightedRoundRobin.getWeight()) {
                weightedRoundRobin.setWeight(weight);
            }

            // 增加当前权重并更新最后访问时间
            long cur = weightedRoundRobin.increaseCurrent();
            weightedRoundRobin.setLastUpdate(now);

            // 选择权重最大的目标服务器
            if (cur > maxCurrent) {
                maxCurrent = cur;
                selectedTarget = target;
                selectedWRR = weightedRoundRobin;
            }

            // 计算总权重
            totalWeight += weight;
        }

        // 回收过期的节点
        // 当目标服务器列表大小与权重映射大小不一致时，清理长时间未使用的节点
        if (targets.size() != map.size()) {
            map.entrySet().removeIf(item -> now - item.getValue().getLastUpdate() > RECYCLE_PERIOD);
        }

        // 选择成功后更新权重
        if (selectedTarget != null) {
            selectedWRR.sel(totalWeight);
            logger.debug("加权轮询选择目标服务器 {}", selectedTarget);
            return selectedTarget;
        }

        // 兜底方案，返回第一个目标服务器
        return targets.getFirst();
    }
}