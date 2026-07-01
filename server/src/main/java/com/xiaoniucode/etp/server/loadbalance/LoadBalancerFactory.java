package com.xiaoniucode.etp.server.loadbalance;

import com.xiaoniucode.etp.core.enums.LoadBalanceType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 负载均衡器工厂
 */
@Component
public class LoadBalancerFactory {
    @Autowired
    private LeastConnLoadBalancer leastConnLoadBalancer;
    @Autowired
    private WeightRoundRobinLoadBalancer weightRoundRobinLoadBalancer;
    @Autowired
    private RandomLoadBalancer randomLoadBalancer;
    @Autowired
    private RoundRobinLoadBalancer roundRobinLoadBalancer;

    /**
     * 获取负载均衡器，如果为空返回一个默认的
     */
    public LoadBalancer getLoadBalancer(LoadBalanceType loadBalanceType) {
        if (loadBalanceType == null) {
            loadBalanceType = LoadBalanceType.ROUND_ROBIN;
        }
        return switch (loadBalanceType) {
            case RANDOM -> randomLoadBalancer;
            case LEAST_CONN -> leastConnLoadBalancer;
            case WEIGHT -> weightRoundRobinLoadBalancer;
            case ROUND_ROBIN -> roundRobinLoadBalancer;
        };
    }
}