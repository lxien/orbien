package io.github.lxien.orbien.server.transport.traffic;

import io.github.lxien.orbien.core.domain.BandwidthConfig;
import io.github.bucket4j.BandwidthBuilder;
import io.github.bucket4j.Bucket;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.time.Duration;

/**
 * 基于 Bucket4j 的代理级带宽令牌桶，按字节平滑消费
 */
public class BandwidthLimiter {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(BandwidthLimiter.class);
    /**
     * 每秒切 20 段，避免 1 秒级突发后长时间归零
     */
    private static final int REFILL_TICKS_PER_SECOND = 20;
    private static final long MAX_SCHEDULE_WAIT_MS = 50;

    private final Bucket uploadBucket;
    private final Bucket downloadBucket;
    private final boolean sharedBucket;

    public BandwidthLimiter(BandwidthConfig config) {
        Long totalBps = config.getTotalBps();
        Long inBps = config.getInBps();
        Long outBps = config.getOutBps();

        if (totalBps != null && inBps == null && outBps == null) {
            Bucket shared = buildBucket(totalBps);
            this.uploadBucket = shared;
            this.downloadBucket = shared;
            this.sharedBucket = true;
            return;
        }
        if (inBps != null && outBps != null) {
            this.uploadBucket = buildBucket(outBps);
            this.downloadBucket = buildBucket(inBps);
            this.sharedBucket = false;
            return;
        }
        if (totalBps != null) {
            if (inBps != null) {
                long out = Math.max(0, totalBps - inBps);
                this.uploadBucket = buildBucket(out);
                this.downloadBucket = buildBucket(inBps);
                this.sharedBucket = false;
                return;
            }
            if (outBps != null) {
                long in = Math.max(0, totalBps - outBps);
                this.uploadBucket = buildBucket(outBps);
                this.downloadBucket = buildBucket(in);
                this.sharedBucket = false;
                return;
            }
        }
        this.uploadBucket = buildBucket(outBps);
        this.downloadBucket = buildBucket(inBps);
        this.sharedBucket = false;
    }

    private static Bucket buildBucket(Long bps) {
        if (bps == null || bps <= 0) {
            return null;
        }
        long bytesPerSecond = Math.max(1, bps / 8);
        long chunk = Math.max(1, bytesPerSecond / REFILL_TICKS_PER_SECOND);
        Duration period = Duration.ofMillis(1000 / REFILL_TICKS_PER_SECOND);
        return Bucket.builder()
                .addLimit(BandwidthBuilder.builder()
                        .capacity(chunk)
                        .refillGreedy(chunk, period)
                        .build())
                .build();
    }

    public boolean isEnabled() {
        return uploadBucket != null || downloadBucket != null;
    }

    /**
     * 尽可能消费令牌，返回实际允许转发的字节数。
     */
    public int consumeUpTo(TrafficDirection direction, int bytes) {
        if (bytes <= 0) {
            return 0;
        }
        Bucket bucket = direction == TrafficDirection.UPLOAD ? uploadBucket : downloadBucket;
        if (bucket == null) {
            return bytes;
        }
        long consumed = bucket.tryConsumeAsMuchAsPossible(bytes);
        if (logger.isDebugEnabled() && consumed < bytes) {
            logger.debug("[限流] 方向={} 请求={} 通过={} 缺口={}",
                    direction, bytes, consumed, bytes - consumed);
        }
        return (int) consumed;
    }

    /**
     * 等待下一枚可用令牌的纳秒数。
     * <p>对超过桶容量的请求，bucket4j 的 {@code estimateAbilityToConsume(n)}
     * 会返回 {@link Long#MAX_VALUE}，因此这里只探测 1 个 token
     */
    public long nanosToWait(TrafficDirection direction) {
        Bucket bucket = direction == TrafficDirection.UPLOAD ? uploadBucket : downloadBucket;
        if (bucket == null) {
            return 0;
        }
        return bucket.estimateAbilityToConsume(1).getNanosToWaitForRefill();
    }

    public long scheduleWaitMs(TrafficDirection direction) {
        long nanos = nanosToWait(direction);
        if (nanos <= 0) {
            return 1;
        }
        long waitMs = Math.max(1, nanos / 1_000_000L);
        return Math.min(waitMs, MAX_SCHEDULE_WAIT_MS);
    }

}
