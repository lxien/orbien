package io.github.lxien.orbien.client.health;

import io.github.lxien.orbien.client.manager.ProxyManagerHolder;
import io.github.lxien.orbien.core.message.Message;
import io.github.lxien.orbien.core.message.TMSP;
import io.github.lxien.orbien.core.message.TMSPFrame;
import io.github.lxien.orbien.core.utils.ProtobufUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.util.*;
import java.util.concurrent.*;

public class HealthCheckManager {
    private final InternalLogger logger = InternalLoggerFactory.getInstance(HealthCheckManager.class);
    private final HealthChecker healthChecker;
    private final ScheduledExecutorService checkScheduler;
    private final ScheduledExecutorService reportScheduler;

    private final Map<String, ScheduledFuture<?>> proxyTasks = new ConcurrentHashMap<>();
    private final Set<ServiceHealth> pendingReports = ConcurrentHashMap.newKeySet();

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

    /**
     * 启动心跳健康检查，如果存在则先取消再启动
     *
     * @param runtimeInfo 代理运行时信息
     */
    public void startHealthCheck(Message.RuntimeInfo runtimeInfo) {
        String proxyId = runtimeInfo.getProxyId();
        stopHealthCheck(proxyId);
        if (!runtimeInfo.hasHealthCheck() || !runtimeInfo.getHealthCheck().getEnabled()) {
            return;
        }
        Message.HealthCheck healthCheck = runtimeInfo.getHealthCheck();
        Runnable checkTask = () -> {
            Message.RuntimeInfo current = ProxyManagerHolder.get().get(proxyId);
            if (current != null) {
                executeHealthCheckForProxy(current);
            }
        };
        ScheduledFuture<?> future = checkScheduler.scheduleAtFixedRate(
                checkTask,
                5,
                healthCheck.getInterval(),
                TimeUnit.SECONDS
        );

        proxyTasks.put(proxyId, future);
        logger.debug("开启代理健康检查: {} 间隔: {} s", proxyId, healthCheck.getInterval());
    }

    public void stopHealthCheck(String proxyId) {
        ScheduledFuture<?> future = proxyTasks.remove(proxyId);
        if (future != null) {
            future.cancel(true);
            logger.debug("停止代理健康检查: {}", proxyId);
        }
        purgePendingReports(proxyId);
    }

    private void purgePendingReports(String proxyId) {
        pendingReports.removeIf(item -> proxyId.equals(item.getProxyId()));
    }

    private boolean isHealthCheckActive(String proxyId) {
        return proxyTasks.containsKey(proxyId);
    }

    private void executeHealthCheckForProxy(Message.RuntimeInfo runtimeInfo) {
        String proxyId = runtimeInfo.getProxyId();
        if (!isHealthCheckActive(proxyId)) {
            return;
        }
        List<Message.Target> targets = runtimeInfo.getTargetsList();
        if (targets.isEmpty()) {
            return;
        }

        List<CompletableFuture<ServiceHealth>> futures = new ArrayList<>();
        Message.HealthCheck healthCheck = runtimeInfo.getHealthCheck();
        for (Message.Target target : targets) {
            CompletableFuture<ServiceHealth> f = healthChecker.check(
                    proxyId,
                    target,
                    healthCheck);
            futures.add(f);
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenRun(() -> {
                    if (!isHealthCheckActive(proxyId)) {
                        return;
                    }
                    List<ServiceHealth> results = new ArrayList<>();
                    for (CompletableFuture<ServiceHealth> f : futures) {
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
        if (pendingReports.isEmpty()) {
            return;
        }
        List<ServiceHealth> toReport = pendingReports.stream()
                .filter(item -> isHealthCheckActive(item.getProxyId()))
                .toList();
        pendingReports.clear();
        if (toReport.isEmpty()) {
            return;
        }
        logger.debug("上报服务健康状态个数：{}", toReport.size());
        List<Message.ServiceHealth> list = toReport.stream().map(serviceHealth -> {
            return Message.ServiceHealth.newBuilder()
                    .setProxyId(serviceHealth.getProxyId())
                    .setHost(serviceHealth.getHost())
                    .setPort(serviceHealth.getPort())
                    .setStatus(serviceHealth.getStatus())
                    .setResponseTimeMs(serviceHealth.getResponseTimeMs() != null ? serviceHealth.getResponseTimeMs() : 0L)
                    .build();
        }).toList();
        Message.BatchReportServiceHealthRequest request = Message.BatchReportServiceHealthRequest
                .newBuilder()
                .addAllItems(list)
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