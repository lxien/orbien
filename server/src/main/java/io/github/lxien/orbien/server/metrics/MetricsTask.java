/*
 *    Copyright 2026 lxien
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package io.github.lxien.orbien.server.metrics;

import io.github.lxien.orbien.server.notify.EventBus;
import io.github.lxien.orbien.server.event.HourlyTrafficEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 流量指标定时任务。
 *
 * <p>负责小时归档持久化与速率刷新，依赖 Spring {@link Scheduled} 调度。
 */
@Component
public class MetricsTask {

    @Autowired
    private MetricsCollector metricsCollector;

    @Autowired
    private EventBus eventBus;

    /**
     * 归档上一自然小时流量并发布 {@link HourlyTrafficEvent}。
     *
     * <p>固定在每小时第 5 分钟执行（{@code 0 5 * * * ?}），错开整点流量尖峰。
     */
    @Scheduled(cron = "0 5 * * * ?")
    public void hourlySnapshotTask() {
        Map<String, ProxyMetrics> allMetrics = metricsCollector.getAllMetrics();
        allMetrics.forEach((proxyId, proxyMetrics) -> {
            HourlyTraffic hourlyTraffic = proxyMetrics.takeHourlySnapshot();
            eventBus.publishAsync(new HourlyTrafficEvent(proxyId,proxyMetrics.getAgentType(), hourlyTraffic));
        });
    }

    /**
     * 刷新所有代理的滑动窗口速率，并推进小时分桶。
     *
     * <p>执行间隔 1 秒（{@code fixedRate = 1000}）。
     */
    @Scheduled(fixedRate = 1000)
    public void rateUpdateTask() {
        Map<String, ProxyMetrics> allMetrics = metricsCollector.getAllMetrics();
        allMetrics.values().forEach(ProxyMetrics::updateRate);
    }
}
