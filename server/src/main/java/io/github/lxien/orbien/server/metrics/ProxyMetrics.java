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

import io.github.lxien.orbien.core.enums.AgentType;
import lombok.Getter;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.LongAdder;

/**
 * 单代理流量计数器。
 *
 * <p>可在隧道 I/O 线程并发更新。{@code read} 为入站（{@code forwardToLocal}），
 * {@code write} 为出站（{@code forwardToRemote}）。小时统计按自然小时分桶，
 * 速率基于 60 秒滑动窗口。
 */
@Getter
public class ProxyMetrics {
    private final String proxyId;
    private final AgentType agentType;
    private static final int HOURS_24 = 24;

    private static final int RATE_WINDOW_SECONDS = 60;

    private static final int RATE_SNAPSHOTS = 60;

    private final AtomicInteger activeChannels = new AtomicInteger(0);
    private final LongAdder totalReadBytes = new LongAdder();
    private final LongAdder totalWriteBytes = new LongAdder();
    private final LongAdder totalReadMessages = new LongAdder();
    private final LongAdder totalWriteMessages = new LongAdder();

    private volatile LocalDateTime lastActiveTime;

    private final ConcurrentHashMap<LocalDateTime, HourlyTraffic> hourlySnapshots = new ConcurrentHashMap<>();

    private volatile HourBucket currentHourBucket;

    private final Object hourLock = new Object();

    private final RateSnapshot[] rateRing = new RateSnapshot[RATE_SNAPSHOTS];
    private final AtomicLong rateIndex = new AtomicLong(0);

    private final AtomicReference<Double> readBytesRate = new AtomicReference<>(0.0);
    private final AtomicReference<Double> writeBytesRate = new AtomicReference<>(0.0);
    private final AtomicReference<Double> readMessagesRate = new AtomicReference<>(0.0);
    private final AtomicReference<Double> writeMessagesRate = new AtomicReference<>(0.0);

    /**
     * @param proxyId 代理标识
     */
    public ProxyMetrics(String proxyId,AgentType agentType) {
        this.proxyId = proxyId;
        this.agentType=agentType;
        this.lastActiveTime = LocalDateTime.now();
        this.currentHourBucket = new HourBucket(LocalDateTime.now().truncatedTo(ChronoUnit.HOURS));
    }

    /**
     * 活跃隧道通道数加一。
     */
    public void incChannels() {
        activeChannels.incrementAndGet();
        lastActiveTime = LocalDateTime.now();
    }

    /**
     * 活跃隧道通道数减一，计数不低于零。
     */
    public void decChannels() {
        int current;
        do {
            current = activeChannels.get();
            if (current <= 0) {
                break;
            }
        } while (!activeChannels.compareAndSet(current, current - 1));
        lastActiveTime = LocalDateTime.now();
    }

    /**
     * 累加入站字节数，同时写入当前小时桶。
     *
     * @param bytes 字节数，不大于零时忽略
     */
    public void incReadBytes(long bytes) {
        if (bytes > 0) {
            recordHourlyTraffic(bytes, 0L, 0L, 0L);
            totalReadBytes.add(bytes);
            lastActiveTime = LocalDateTime.now();
        }
    }

    /**
     * 累加出站字节数，同时写入当前小时桶。
     *
     * @param bytes 字节数，不大于零时忽略
     */
    public void incWriteBytes(long bytes) {
        if (bytes > 0) {
            recordHourlyTraffic(0L, bytes, 0L, 0L);
            totalWriteBytes.add(bytes);
            lastActiveTime = LocalDateTime.now();
        }
    }

    /**
     * 累加入站消息数，同时写入当前小时桶。
     *
     * @param count 消息数，不大于零时忽略
     */
    public void incReadMessages(long count) {
        if (count > 0) {
            recordHourlyTraffic(0L, 0L, count, 0L);
            totalReadMessages.add(count);
            lastActiveTime = LocalDateTime.now();
        }
    }

    /**
     * 累加出站消息数，同时写入当前小时桶。
     *
     * @param count 消息数，不大于零时忽略
     */
    public void incWriteMessages(long count) {
        if (count > 0) {
            recordHourlyTraffic(0L, 0L, 0L, count);
            totalWriteMessages.add(count);
            lastActiveTime = LocalDateTime.now();
        }
    }

    /**
     * 将增量写入当前小时桶；若系统时钟已进入新的小时，先完成归档切换。
     */
    private void recordHourlyTraffic(long readBytes, long writeBytes, long readMessages, long writeMessages) {
        LocalDateTime nowHour = LocalDateTime.now().truncatedTo(ChronoUnit.HOURS);
        HourBucket bucket = currentHourBucket;
        if (bucket == null || !bucket.hour.equals(nowHour)) {
            bucket = rollHourIfNeeded(nowHour);
        }
        if (readBytes > 0) {
            bucket.readBytes.add(readBytes);
        }
        if (writeBytes > 0) {
            bucket.writeBytes.add(writeBytes);
        }
        if (readMessages > 0) {
            bucket.readMessages.add(readMessages);
        }
        if (writeMessages > 0) {
            bucket.writeMessages.add(writeMessages);
        }
    }

    /**
     * 取出上一自然小时的归档数据，供定时任务持久化。
     *
     * <p>调用前会先推进小时分桶，确保跨越整点后归档状态一致。
     *
     * @return 上一小时流量；无记录时各指标为 0
     */
    public HourlyTraffic takeHourlySnapshot() {
        synchronized (hourLock) {
            LocalDateTime nowHour = LocalDateTime.now().truncatedTo(ChronoUnit.HOURS);
            rollHourLocked(nowHour);
            LocalDateTime persistHour = nowHour.minusHours(1);
            return hourlySnapshots.getOrDefault(persistHour,
                    new HourlyTraffic(persistHour, 0L, 0L, 0L, 0L));
        }
    }

    /**
     * 推进小时分桶，采样累计值并更新速率。
     *
     * <p>须由 {@link MetricsTask} 每秒调用一次。小时归档在 {@code hourLock} 内完成，
     * 速率环写入无锁，二者可并行。
     */
    public void updateRate() {
        synchronized (hourLock) {
            rollHourLocked(LocalDateTime.now().truncatedTo(ChronoUnit.HOURS));
        }

        long nowReadBytes = totalReadBytes.sum();
        long nowWriteBytes = totalWriteBytes.sum();
        long nowReadMsg = totalReadMessages.sum();
        long nowWriteMsg = totalWriteMessages.sum();

        RateSnapshot snapshot = new RateSnapshot(
                System.currentTimeMillis(),
                nowReadBytes, nowWriteBytes, nowReadMsg, nowWriteMsg
        );

        long idx = rateIndex.getAndIncrement();
        rateRing[(int) (idx % RATE_SNAPSHOTS)] = snapshot;

        readBytesRate.set(computeRate(true, true));
        writeBytesRate.set(computeRate(false, true));
        readMessagesRate.set(computeRate(true, false));
        writeMessagesRate.set(computeRate(false, false));
    }

    /**
     * 在速率环中查找窗口起点，计算指定维度的瞬时速率。
     *
     * <p>窗口长度为 {@link #RATE_WINDOW_SECONDS} 秒。样本不足时回退到环中最早快照；
     * 仍无有效样本则返回 0。
     *
     * @param isRead  {@code true} 取入站，{@code false} 取出站
     * @param isBytes {@code true} 取字节，{@code false} 取消息
     * @return 速率，单位：字节/秒或条/秒
     */
    private double computeRate(boolean isRead, boolean isBytes) {
        long end = rateIndex.get();
        if (end < 2) {
            return 0.0;
        }

        long now = System.currentTimeMillis();
        long windowMs = RATE_WINDOW_SECONDS * 1000L;

        RateSnapshot latest = getRateSnapshot(end - 1);
        if (latest == null) {
            return 0.0;
        }

        RateSnapshot target = null;
        long start = Math.max(0, end - RATE_SNAPSHOTS);

        for (long i = end - 1; i >= start; i--) {
            RateSnapshot snap = getRateSnapshot(i);
            if (snap == null) {
                continue;
            }
            if (now - snap.getTimestamp() >= windowMs) {
                target = snap;
                break;
            }
        }

        if (target == null) {
            target = getRateSnapshot(start);
            if (target == null) {
                return 0.0;
            }
        }

        long latestVal = getValue(latest, isRead, isBytes);
        long targetVal = getValue(target, isRead, isBytes);
        long delta = latestVal - targetVal;
        long deltaTime = latest.getTimestamp() - target.getTimestamp();

        if (deltaTime <= 0) {
            return 0.0;
        }

        return (double) delta * 1000.0 / deltaTime;
    }

    /**
     * @param snap    速率快照
     * @param isRead  {@code true} 取入站，{@code false} 取出站
     * @param isBytes {@code true} 取字节，{@code false} 取消息
     * @return 快照中对应维度的累计值
     */
    private long getValue(RateSnapshot snap, boolean isRead, boolean isBytes) {
        if (isBytes) {
            return isRead ? snap.getReadBytes() : snap.getWriteBytes();
        }
        return isRead ? snap.getReadMessages() : snap.getWriteMessages();
    }

    /**
     * @param seq 逻辑序号
     * @return 序号对应的环槽快照，槽位为空时返回 {@code null}
     */
    private RateSnapshot getRateSnapshot(long seq) {
        return rateRing[(int) (seq % RATE_SNAPSHOTS)];
    }

    /**
     * 返回以当前时刻为终点的 24 个自然小时流量。
     *
     * <p>调用前推进小时分桶。末项读取当前小时桶的实时值，其余从归档 Map 取值；
     * 无记录的小时以零值填充。列表按时间升序排列。
     *
     * @return 长度恒为 24 的列表
     */
    public List<HourlyTraffic> getHourlyStatsLast24h() {
        synchronized (hourLock) {
            LocalDateTime nowHour = LocalDateTime.now().truncatedTo(ChronoUnit.HOURS);
            rollHourLocked(nowHour);

            List<HourlyTraffic> result = new ArrayList<>(HOURS_24);
            for (int i = HOURS_24 - 1; i >= 0; i--) {
                LocalDateTime hour = nowHour.minusHours(i);
                if (hour.equals(currentHourBucket.hour)) {
                    result.add(currentHourBucket.toHourlyTraffic());
                } else {
                    result.add(hourlySnapshots.getOrDefault(hour,
                            new HourlyTraffic(hour, 0L, 0L, 0L, 0L)));
                }
            }
            return result;
        }
    }

    /**
     * 检测小时边界，必要时在锁内完成归档切换。
     *
     * @param nowHour 当前整点小时
     * @return 与 {@code nowHour} 对齐的当前小时桶
     */
    private HourBucket rollHourIfNeeded(LocalDateTime nowHour) {
        synchronized (hourLock) {
            rollHourLocked(nowHour);
            return currentHourBucket;
        }
    }

    /**
     * 将早于 {@code nowHour} 的桶依次写入 {@link #hourlySnapshots}。
     *
     * <p>代理长时间无流量时，中间空缺的小时以零值桶补全后归档。
     * 调用方须持有 {@link #hourLock}。
     *
     * @param nowHour 当前整点小时
     */
    private void rollHourLocked(LocalDateTime nowHour) {
        HourBucket bucket = currentHourBucket;
        while (bucket != null && bucket.hour.isBefore(nowHour)) {
            hourlySnapshots.put(bucket.hour, bucket.toHourlyTraffic());
            bucket = new HourBucket(bucket.hour.plusHours(1));
            currentHourBucket = bucket;
        }
        if (currentHourBucket == null) {
            currentHourBucket = new HourBucket(nowHour);
        }
        pruneOldSnapshots(nowHour);
    }

    /**
     * 移除超出 24 小时展示窗口的归档条目。
     *
     * @param nowHour 当前整点小时
     */
    private void pruneOldSnapshots(LocalDateTime nowHour) {
        LocalDateTime threshold = nowHour.minusHours(HOURS_24 + 1);
        hourlySnapshots.keySet().removeIf(hour -> hour.isBefore(threshold));
    }

    /**
     * 将内存计数器转为查询用 DTO。
     *
     * @return 包含累计值与当前速率的快照
     */
    public Metrics toMetrics() {
        Metrics m = new Metrics();
        m.setProxyId(proxyId);
        m.setActiveChannels(activeChannels.get());
        m.setReadBytes(totalReadBytes.sum());
        m.setWriteBytes(totalWriteBytes.sum());
        m.setReadMessages(totalReadMessages.sum());
        m.setWriteMessages(totalWriteMessages.sum());
        m.setReadRate(readBytesRate.get());
        m.setWriteRate(writeBytesRate.get());
        m.setLastActiveTime(lastActiveTime);
        return m;
    }

    /**
     * 当前自然小时的内存累加桶。
     */
    private static final class HourBucket {
        private final LocalDateTime hour;
        private final LongAdder readBytes = new LongAdder();
        private final LongAdder writeBytes = new LongAdder();
        private final LongAdder readMessages = new LongAdder();
        private final LongAdder writeMessages = new LongAdder();

        /**
         * @param hour 桶所属整点小时
         */
        private HourBucket(LocalDateTime hour) {
            this.hour = hour;
        }

        /**
         * @return 当前桶累计值构成的 {@link HourlyTraffic}
         */
        private HourlyTraffic toHourlyTraffic() {
            return new HourlyTraffic(
                    hour,
                    readBytes.sum(),
                    writeBytes.sum(),
                    readMessages.sum(),
                    writeMessages.sum()
            );
        }
    }
}
