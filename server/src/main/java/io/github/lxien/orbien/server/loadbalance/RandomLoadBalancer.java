package io.github.lxien.orbien.server.loadbalance;

import io.github.lxien.orbien.core.domain.Target;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
@Component
public class RandomLoadBalancer implements LoadBalancer{
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(RandomLoadBalancer.class);

    public RandomLoadBalancer() {
        logger.debug("创建随机负载均衡器");
    }

    @Override
    public Target select(String proxyId,List<Target> targets) {
        if (targets == null || targets.isEmpty()) {
            logger.debug("目标服务器列表为空");
            return null;
        }
        int index = ThreadLocalRandom.current().nextInt(targets.size());
        Target selectedTarget = targets.get(index);
        logger.debug("随机选择目标服务器 {}", selectedTarget);
        return selectedTarget;
    }
}
