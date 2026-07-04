package io.github.lxien.orbien.server.loadbalance;

import io.github.lxien.orbien.core.domain.Target;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
@Component
public class RoundRobinLoadBalancer implements LoadBalancer{
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(RoundRobinLoadBalancer.class);
    private final AtomicInteger currentIndex;

    public RoundRobinLoadBalancer() {
        this.currentIndex = new AtomicInteger(new Random().nextInt(1000));
        logger.debug("创建轮询负载均衡器");
    }

    @Override
    public Target select(String proxyId,List<Target> targets) {
        if (targets == null || targets.isEmpty()) {
            logger.warn("目标服务器列表为空");
            return null;
        }

        if (targets.size() == 1) {
            Target selectedTarget = targets.getFirst();
            logger.debug("只有一个目标服务器，直接选择 {}", selectedTarget);
            return selectedTarget;
        }

        int pos = currentIndex.incrementAndGet() & Integer.MAX_VALUE;
        int index = pos % targets.size();
        Target selectedTarget = targets.get(index);
        logger.debug("轮询选择目标服务器 {}", selectedTarget);
        return selectedTarget;
    }
}
