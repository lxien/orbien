package io.github.lxien.orbien.server.transport;

import io.github.lxien.orbien.core.domain.BandwidthConfig;
import io.github.bucket4j.BandwidthBuilder;
import io.github.bucket4j.Bucket;
import io.netty.buffer.ByteBuf;

import java.time.Duration;

/**
 * 带宽限流器
 */
public class BandwidthLimiter {
    private final Bucket uploadBucket;
    private final Bucket downloadBucket;

    public BandwidthLimiter(BandwidthConfig config) {
        Long totalBps = config.getTotalBps();
        Long inBps = config.getInBps();
        Long outBps = config.getOutBps();

        //只配置 total（共享桶）
        if (totalBps != null && inBps == null && outBps == null) {
            Bucket shared = buildBucket(totalBps);
            this.uploadBucket = shared;
            this.downloadBucket = shared;
            return;
        }

        // 三个都配置，忽略 total
        if (inBps != null && outBps != null) {
            this.uploadBucket = buildBucket(outBps);
            this.downloadBucket = buildBucket(inBps);
            return;
        }

        // partial + total
        if (totalBps != null) {

            if (inBps != null) {
                long out = Math.max(0, totalBps - inBps);
                this.uploadBucket = buildBucket(out);
                this.downloadBucket = buildBucket(inBps);
                return;
            }
            if (outBps != null) {
                long in = Math.max(0, totalBps - outBps);
                this.uploadBucket = buildBucket(outBps);
                this.downloadBucket = buildBucket(in);
                return;
            }
        }

        // fallback（只配单方向）
        this.uploadBucket = buildBucket(outBps);
        this.downloadBucket = buildBucket(inBps);
    }

    /**
     * 构建 bucket（bps → byte/s）
     */
    private Bucket buildBucket(Long bps) {
        if (bps == null || bps <= 0) {
            return null;
        }

        long bytesPerSecond = Math.max(1, bps / 8);

        return Bucket.builder()
                .addLimit(
                        BandwidthBuilder.builder()
                                .capacity(bytesPerSecond)
                                .refillGreedy(
                                        bytesPerSecond,
                                        Duration.ofSeconds(1)
                                )
                                .build()
                )
                .build();
    }    public boolean tryUpload(ByteBuf msg) {
        if (uploadBucket == null) return true;
        return uploadBucket.tryConsume(msg.readableBytes());
    }

    public boolean tryDownload(ByteBuf msg) {
        if (downloadBucket == null) return true;
        return downloadBucket.tryConsume(msg.readableBytes());
    }

    public long getDownloadWaitNanos(int bytes) {
        if (downloadBucket == null) return 0;
        return downloadBucket.estimateAbilityToConsume(bytes)
                .getNanosToWaitForRefill();
    }

    public long getUploadWaitNanos(int bytes) {
        if (uploadBucket == null) return 0;
        return uploadBucket.estimateAbilityToConsume(bytes)
                .getNanosToWaitForRefill();
    }
}