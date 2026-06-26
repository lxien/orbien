/*
 *    Copyright 2026 xiaoniucode
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

package com.xiaoniucode.etp.server.metrics;

import com.xiaoniucode.etp.common.message.PageResult;
import com.xiaoniucode.etp.core.enums.AgentType;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

@Component
public class MetricsCollector {
    private final InternalLogger logger = InternalLoggerFactory.getInstance(MetricsCollector.class);
    private static final Map<String, ProxyMetrics> PROXY_METRICS = new ConcurrentHashMap<>(256);

    public ProxyMetrics getOrCreate(String proxyId, AgentType agentType) {
        if (!StringUtils.hasText(proxyId) || agentType == null) {
            throw new IllegalArgumentException("proxyId or agentType not null");
        }
        return PROXY_METRICS.computeIfAbsent(proxyId, id -> new ProxyMetrics(id, agentType));
    }

    public void onChannelActive(String proxyId, AgentType agentType) {
        ProxyMetrics m = getOrCreate(proxyId, agentType);
        if (m != null) {
            m.incChannels();
        }
    }

    public void onChannelInactive(String proxyId) {
        if (!StringUtils.hasText(proxyId)) {
            return;
        }
        ProxyMetrics m = PROXY_METRICS.get(proxyId);
        if (m != null) {
            m.decChannels();
        }
    }

    public void collect(String proxyId, Consumer<ProxyMetrics> action) {
        if (!StringUtils.hasText(proxyId) || action == null) {
            return;
        }
        ProxyMetrics m = PROXY_METRICS.get(proxyId);
        if (m != null) {
            try {
                action.accept(m);
            } catch (Exception e) {
                logger.warn("Error collecting metrics for proxy: {}", proxyId, e);
            }
        }
    }

    public Map<String, ProxyMetrics> getAllMetrics() {
        return Collections.unmodifiableMap(new ConcurrentHashMap<>(PROXY_METRICS));
    }

    public boolean removeByProxyId(String proxyId) {
        if (!StringUtils.hasText(proxyId)) {
            return false;
        }
        ProxyMetrics m = PROXY_METRICS.remove(proxyId);
        if (m != null) {
            logger.debug("已移除代理 {} 的流量统计记录", proxyId);
            return true;
        }
        return false;
    }

    public Metrics getProxyMetrics(String proxyId) {
        if (!StringUtils.hasText(proxyId)) {
            return null;
        }
        ProxyMetrics pm = PROXY_METRICS.get(proxyId);
        return pm != null ? pm.toMetrics() : null;
    }

    public PageResult<Metrics> listAllMetrics(int page, int size) {
        List<Metrics> pageData = PROXY_METRICS.values().stream()
                .sorted(Comparator.comparingLong(m ->
                        -(m.getTotalReadBytes().sum() + m.getTotalWriteBytes().sum())))
                .skip((long) Math.max(0, page) * Math.clamp(size, 1, 100))
                .limit(Math.clamp(size, 1, 100))
                .map(ProxyMetrics::toMetrics)
                .toList();

        int pageSize = Math.clamp(size, 1, 100);
        return new PageResult<>(pageData, (long) PROXY_METRICS.size(),
                Math.max(0, page), pageSize);
    }

    public List<HourlyTraffic> get24hTraffic(String proxyId) {
        if (!StringUtils.hasText(proxyId)) {
            return Collections.emptyList();
        }
        ProxyMetrics pm = PROXY_METRICS.get(proxyId);
        if (pm == null) {
            return Collections.emptyList();
        }
        return pm.getHourlyStatsLast24h();
    }

    /**
     * 按小时下标对齐后，汇总所有代理最近 24 小时流量。
     *
     * @return 聚合后的小时流量列表
     */
    public List<HourlyTraffic> getTotal24hTraffic() {
        if (PROXY_METRICS.isEmpty()) {
            return Collections.emptyList();
        }

        List<List<HourlyTraffic>> allTraffic = new ArrayList<>();
        try {
            for (ProxyMetrics pm : PROXY_METRICS.values()) {
                List<HourlyTraffic> traffic = pm.getHourlyStatsLast24h();
                if (traffic != null && traffic.size() == 24) {
                    allTraffic.add(traffic);
                }
            }
        } catch (Exception e) {
            logger.warn("Error collecting total 24h traffic", e);
            return Collections.emptyList();
        }

        if (allTraffic.isEmpty()) {
            return Collections.emptyList();
        }

        List<HourlyTraffic> result = new ArrayList<>(24);
        for (int i = 0; i < 24; i++) {
            long sumReadBytes = 0, sumWriteBytes = 0;
            long sumReadMsg = 0, sumWriteMsg = 0;
            LocalDateTime hourTime = null;

            for (List<HourlyTraffic> traffic : allTraffic) {
                HourlyTraffic item = traffic.get(i);
                if (item == null) {
                    continue;
                }

                if (hourTime == null) {
                    hourTime = item.getHour();
                }
                sumReadBytes += item.getReadBytes();
                sumWriteBytes += item.getWriteBytes();
                sumReadMsg += item.getReadMessages();
                sumWriteMsg += item.getWriteMessages();
            }

            result.add(new HourlyTraffic(hourTime, sumReadBytes, sumWriteBytes,
                    sumReadMsg, sumWriteMsg));
        }

        return result;
    }

    /**
     * @return 所有代理入站字节速率之和，单位：字节/秒
     */
    public double getTotalReadBytesRate() {
        if (PROXY_METRICS.isEmpty()) {
            return 0.0;
        }
        return PROXY_METRICS.values().stream()
                .mapToDouble(m -> m.getReadBytesRate().get())
                .sum();
    }

    /**
     * @return 所有代理出站字节速率之和，单位：字节/秒
     */
    public double getTotalWriteBytesRate() {
        if (PROXY_METRICS.isEmpty()) {
            return 0.0;
        }
        return PROXY_METRICS.values().stream()
                .mapToDouble(m -> m.getWriteBytesRate().get())
                .sum();
    }

    /**
     * @return 所有代理入站消息速率之和，单位：条/秒
     */
    public double getTotalReadMessagesRate() {
        if (PROXY_METRICS.isEmpty()) {
            return 0.0;
        }
        return PROXY_METRICS.values().stream()
                .mapToDouble(m -> m.getReadMessagesRate().get())
                .sum();
    }

    /**
     * @return 所有代理出站消息速率之和，单位：条/秒
     */
    public double getTotalWriteMessagesRate() {
        if (PROXY_METRICS.isEmpty()) {
            return 0.0;
        }
        return PROXY_METRICS.values().stream()
                .mapToDouble(m -> m.getWriteMessagesRate().get())
                .sum();
    }

    /**
     * @return 所有代理入站字节累计值之和
     */
    public long getTotalReadBytes() {
        if (PROXY_METRICS.isEmpty()) {
            return 0L;
        }
        return PROXY_METRICS.values().stream()
                .mapToLong(m -> m.getTotalReadBytes().sum())
                .sum();
    }

    /**
     * @return 所有代理出站字节累计值之和
     */
    public long getTotalWriteBytes() {
        if (PROXY_METRICS.isEmpty()) {
            return 0L;
        }
        return PROXY_METRICS.values().stream()
                .mapToLong(m -> m.getTotalWriteBytes().sum())
                .sum();
    }

    /**
     * @return 所有代理入站消息累计值之和
     */
    public long getTotalReadMessages() {
        if (PROXY_METRICS.isEmpty()) {
            return 0L;
        }
        return PROXY_METRICS.values().stream()
                .mapToLong(m -> m.getTotalReadMessages().sum())
                .sum();
    }

    /**
     * @return 所有代理出站消息累计值之和
     */
    public long getTotalWriteMessages() {
        if (PROXY_METRICS.isEmpty()) {
            return 0L;
        }
        return PROXY_METRICS.values().stream()
                .mapToLong(m -> m.getTotalWriteMessages().sum())
                .sum();
    }

    /**
     * @return 所有代理活跃隧道通道数之和
     */
    public int getTotalActiveChannels() {
        if (PROXY_METRICS.isEmpty()) {
            return 0;
        }
        return PROXY_METRICS.values().stream()
                .mapToInt(m -> m.getActiveChannels().get())
                .sum();
    }

    /**
     * @param proxyId 代理标识
     * @return 活跃通道数；标识无效或代理不存在时返回 0
     */
    public int getActiveChannels(String proxyId) {
        if (!StringUtils.hasText(proxyId)) {
            return 0;
        }
        ProxyMetrics proxyMetrics = PROXY_METRICS.get(proxyId);
        return proxyMetrics != null ? proxyMetrics.getActiveChannels().get() : 0;
    }

    /**
     * @return 当前持有计数器的代理数量
     */
    public int getTotalProxyCount() {
        return PROXY_METRICS.size();
    }

    /**
     * 清空全部内存计数器。
     */
    public void clearAll() {
        PROXY_METRICS.clear();
        logger.info("已清空所有代理流量统计数据");
    }

    /**
     * 列出 {@link ProxyMetrics#getLastActiveTime()} 早于阈值的代理。
     *
     * @param inactiveDurationMinutes 空闲阈值，单位：分钟
     * @return 代理标识列表
     */
    public List<String> getInactiveProxies(long inactiveDurationMinutes) {
        if (PROXY_METRICS.isEmpty()) {
            return Collections.emptyList();
        }

        LocalDateTime threshold = LocalDateTime.now().minusMinutes(inactiveDurationMinutes);
        return PROXY_METRICS.entrySet().stream()
                .filter(entry -> entry.getValue().getLastActiveTime().isBefore(threshold))
                .map(Map.Entry::getKey)
                .toList();
    }

    /**
     * 移除空闲超过阈值的代理计数器。
     *
     * @param inactiveDurationMinutes 空闲阈值，单位：分钟
     * @return 实际移除的数量
     */
    public int cleanupInactiveProxies(long inactiveDurationMinutes) {
        List<String> inactiveProxies = getInactiveProxies(inactiveDurationMinutes);
        for (String proxyId : inactiveProxies) {
            removeByProxyId(proxyId);
        }
        if (!inactiveProxies.isEmpty()) {
            logger.info("清理了 {} 个不活动的代理", inactiveProxies.size());
        }
        return inactiveProxies.size();
    }
}
