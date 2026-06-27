package com.xiaoniucode.etp.client.health;

import com.xiaoniucode.etp.common.utils.StringUtils;
import com.xiaoniucode.etp.core.domain.HealthCheckConfig;
import com.xiaoniucode.etp.core.domain.ProxyConfig;
import com.xiaoniucode.etp.core.domain.Target;
import com.xiaoniucode.etp.core.message.Message;
import com.xiaoniucode.etp.core.message.TMSP;
import com.xiaoniucode.etp.core.message.TMSPFrame;
import com.xiaoniucode.etp.core.utils.ProtobufUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

public class HealthCheckManager {
    private final InternalLogger logger = InternalLoggerFactory.getInstance(HealthCheckManager.class);
    private final HealthChecker healthChecker;
    private final ScheduledExecutorService checkScheduler;
    private final ScheduledExecutorService reportScheduler;

    private final Map<String, ScheduledFuture<?>> proxyTasks = new ConcurrentHashMap<>();
    private final List<Message.ServiceHealth> pendingReports = new CopyOnWriteArrayList<>();

    private final Channel control;

    public HealthCheckManager(Channel control) {
        this.healthChecker = new HealthChecker();
        this.control = control;
        this.checkScheduler = Executors.newScheduledThreadPool(4, r -> {
            Thread t = new Thread(r, "HealthCheck-Worker");
            t.setDaemon(true);
            return t;
        });
        this.reportScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "HealthReport-Worker");
            t.setDaemon(true);
            return t;
        });

        reportScheduler.scheduleAtFixedRate(this::reportHealthBatch, 6, 8, TimeUnit.SECONDS);
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown, "HealthCheck-Shutdown"));
    }


    public void startHealthCheck(ProxyConfig proxy) {
        HealthCheckConfig config = proxy.getHealthCheck();
        if (config == null || !config.isEnabled()) {
            return;
        }

        String proxyId = proxy.getProxyId();
        if (!StringUtils.hasText(proxyId)) {
            logger.debug("代理 {} 启动健康检查失败，代理ID为空", proxy.getName());
            return;
        }
        stopHealthCheck(proxyId);
        Runnable checkTask = () -> executeHealthCheckForProxy(proxy);

        ScheduledFuture<?> future = checkScheduler.scheduleAtFixedRate(
                checkTask,
                5,
                config.getInterval(),
                TimeUnit.SECONDS
        );

        proxyTasks.put(proxyId, future);
        logger.debug("开启代理健康检查: {} 间隔: {} s", proxy.getName(), config.getInterval());
    }

    public void stopHealthCheck(String proxyId) {
        ScheduledFuture<?> future = proxyTasks.remove(proxyId);
        if (future != null) {
            future.cancel(true);
            logger.debug("停止代理健康检查: {}", proxyId);
        }
    }


    private void executeHealthCheckForProxy(ProxyConfig proxy) {
        List<Target> targets = proxy.getTargets();
        if (targets == null || targets.isEmpty()) return;

        List<CompletableFuture<Message.ServiceHealth>> futures = new ArrayList<>();

        for (Target target : targets) {
            CompletableFuture<Message.ServiceHealth> f = healthChecker.check(
                    proxy.getProxyId(),
                    proxy.getProtocol(),
                    target,
                    proxy.getHealthCheck());
            futures.add(f);
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenRun(() -> {
                    List<Message.ServiceHealth> results = new ArrayList<>();
                    for (CompletableFuture<Message.ServiceHealth> f : futures) {
                        try {
                            results.add(f.get());
                        } catch (Exception e) {
                            logger.error(e);
                        }
                    }
                    pendingReports.addAll(results);

                    if (pendingReports.size() >= 10) {
                        reportHealthBatch();
                    }
                });
    }

    private void reportHealthBatch() {
        if (pendingReports.isEmpty()) return;
        List<Message.ServiceHealth> toReport = new ArrayList<>(pendingReports);
        logger.debug("上报服务健康状态个数：{}", toReport.size());
        pendingReports.clear();
        Message.BatchReportServiceHealthRequest request = Message.BatchReportServiceHealthRequest
                .newBuilder()
                .addAllItems(toReport)
                .build();

        ByteBuf buf = ProtobufUtil.toByteBuf(request, control.alloc());
        TMSPFrame frame = new TMSPFrame(0, TMSP.MSG_SERVICE_HEALTH_REPORT, buf);
        control.writeAndFlush(frame);
    }

    public void shutdown() {
        proxyTasks.values().forEach(f -> f.cancel(true));
        checkScheduler.shutdown();
        reportScheduler.shutdown();
        healthChecker.shutdown();
        logger.debug("健康检查管理器已停止所有任务");
    }
}