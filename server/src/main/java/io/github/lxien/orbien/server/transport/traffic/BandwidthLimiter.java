package io.github.lxien.orbien.server.transport.traffic;

import io.github.lxien.orbien.core.domain.BandwidthConfig;
import io.github.bucket4j.BandwidthBuilder;
import io.github.bucket4j.Bucket;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.time.Duration;

/**
 * 基于 Bucket4j 的代理级带宽令牌桶，按字节平滑消费。
 */
public class BandwidthLimiter {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(BandwidthLimiter.class);

    private static final int REFILL_TICKS_PER_SECOND = 20;

    private static final long MAX_SCHEDULE_WAIT_MS = 1000L / REFILL_TICKS_PER_SECOND;
    private static final long MIN_BURST_BYTES = 16 * 1024L;

    private final Bucket uploadBucket;
    private final Bucket downloadBucket;
    private final boolean sharedBucket;
    private final long bytesPerSecond;

    public BandwidthLimiter(BandwidthConfig config) {
        Long totalBps = config.getTotalBps();
        Long inBps = config.getInBps();
        Long outBps = config.getOutBps();

        if (totalBps != null && inBps == null && outBps == null) {
            Bucket shared = buildBucket(totalBps);
            this.uploadBucket = shared;
            this.downloadBucket = shared;
            this.sharedBucket = true;
            this.bytesPerSecond = toBytesPerSecond(totalBps);
            return;
        }
        if (inBps != null && outBps != null) {
            this.uploadBucket = buildBucket(outBps);
            this.downloadBucket = buildBucket(inBps);
            this.sharedBucket = false;
            this.bytesPerSecond = Math.max(toBytesPerSecond(inBps), toBytesPerSecond(outBps));
            return;
        }
        if (totalBps != null) {
            if (inBps != null) {
                long out = Math.max(0, totalBps - inBps);
                this.uploadBucket = buildBucket(out);
                this.downloadBucket = buildBucket(inBps);
                this.sharedBucket = false;
                this.bytesPerSecond = toBytesPerSecond(totalBps);
                return;
            }
            if (outBps != null) {
                long in = Math.max(0, totalBps - outBps);
                this.uploadBucket = buildBucket(outBps);
                this.downloadBucket = buildBucket(in);
                this.sharedBucket = false;
                this.bytesPerSecond = toBytesPerSecond(totalBps);
                return;
            }
        }
        this.uploadBucket = buildBucket(outBps);
        this.downloadBucket = buildBucket(inBps);
        this.sharedBucket = false;
        this.bytesPerSecond = Math.max(toBytesPerSecond(inBps), toBytesPerSecond(outBps));
    }

    private static long toBytesPerSecond(Long bps) {
        if (bps == null || bps <= 0) {
            return 0;
        }
        return Math.max(1, bps / 8);
    }

    private static Bucket buildBucket(Long bps) {
        if (bps == null || bps <= 0) {
            return null;
        }
        long bytesPerSecond = toBytesPerSecond(bps);
        // 容量 = 整秒带宽（不低于 MIN_BURST），回补按 tick 切分
        long capacity = Math.max(MIN_BURST_BYTES, bytesPerSecond);
        long refillChunk = Math.max(1, bytesPerSecond / REFILL_TICKS_PER_SECOND);
        Duration period = Duration.ofMillis(1000 / REFILL_TICKS_PER_SECOND);
        return Bucket.builder()
                .addLimit(BandwidthBuilder.builder()
                        .capacity(capacity)
                        .refillGreedy(refillChunk, period)
                        .initialTokens(capacity)
                        .build())
                .build();
    }

    public boolean isEnabled() {
        return uploadBucket != null || downloadBucket != null;
    }

    public boolean isSharedBucket() {
        return sharedBucket;
    }

    public long getBytesPerSecond() {
        return bytesPerSecond;
    }


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
