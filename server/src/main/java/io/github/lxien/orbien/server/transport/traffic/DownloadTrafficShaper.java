package io.github.lxien.orbien.server.transport.traffic;

import io.netty.buffer.ByteBuf;
import io.netty.channel.EventLoop;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;


final class DownloadTrafficShaper {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(DownloadTrafficShaper.class);

    private static final long MIN_QUEUE_BYTES = 2 * 1024 * 1024L;
    private static final long MAX_QUEUE_BYTES_CAP = 16 * 1024 * 1024L;
    private static final int MAX_QUEUE_FRAMES = 4096;

    private static final long EMERGENCY_DROP_BYTES = 32 * 1024 * 1024L;

    private final BandwidthLimiter limiter;
    private final TrafficBufferQueue queue = new TrafficBufferQueue();
    private final AtomicBoolean drainScheduled = new AtomicBoolean();
    private final AtomicInteger epoch = new AtomicInteger();
    private final DownloadCallbacks callbacks;
    private final long maxQueueBytes;
    private final long highWaterBytes;
    private final long lowWaterBytes;

    DownloadTrafficShaper(BandwidthLimiter limiter, DownloadCallbacks callbacks) {
        this.limiter = limiter;
        this.callbacks = callbacks;
        long bps = limiter != null ? Math.max(0, limiter.getBytesPerSecond()) : 0;
        // 约 8 秒带宽，夹在 [2MB, 16MB]
        this.maxQueueBytes = Math.min(MAX_QUEUE_BYTES_CAP, Math.max(MIN_QUEUE_BYTES, bps * 8));
        this.highWaterBytes = Math.max(256 * 1024L, maxQueueBytes / 2);
        this.lowWaterBytes = Math.max(64 * 1024L, maxQueueBytes / 8);
    }

    /**
     * 接管并整形已拥有的 payload，必须在 visitor EventLoop 上调用
     */
    void offerOwned(ByteBuf payload) {
        if (payload == null || !payload.isReadable()) {
            ReferenceCountUtil.release(payload);
            return;
        }
        if (limiter == null || !limiter.isEnabled()) {
            callbacks.forwardDownload(payload, false);
            return;
        }
        if (callbacks.isAborted()) {
            ReferenceCountUtil.release(payload);
            return;
        }

        if (!queue.isEmpty()) {
            enqueueAll(payload);
            return;
        }

        int readable = payload.readableBytes();
        int readerIndex = payload.readerIndex();
        int allowed = limiter.consumeUpTo(TrafficDirection.DOWNLOAD, readable);
        if (allowed >= readable) {
            callbacks.forwardDownload(payload, false);
            return;
        }
        if (allowed > 0) {
            ByteBuf slice = payload.retainedSlice(readerIndex, allowed);
            callbacks.forwardDownload(slice, false);
        }
        int remainderBytes = readable - allowed;
        if (remainderBytes <= 0) {
            ReferenceCountUtil.release(payload);
            return;
        }
        int remainderIndex = readerIndex + allowed;
        ByteBuf queued = payload.retainedSlice(remainderIndex, remainderBytes);
        ReferenceCountUtil.release(payload);
        enqueueAll(queued);
    }

    private void enqueueAll(ByteBuf payload) {
        if (payload == null || !payload.isReadable()) {
            ReferenceCountUtil.release(payload);
            return;
        }
        if (callbacks.isAborted()) {
            ReferenceCountUtil.release(payload);
            return;
        }
        int enqueuedBytes = payload.readableBytes();
        if (queue.offer(payload, MAX_QUEUE_FRAMES, maxQueueBytes)) {
            afterEnqueue(enqueuedBytes);
            return;
        }
        if (queue.bytes() + enqueuedBytes <= EMERGENCY_DROP_BYTES
                && queue.offer(payload, MAX_QUEUE_FRAMES * 2, EMERGENCY_DROP_BYTES)) {
            logger.warn("[限流][下载] 队列超过软上限，继续背压 streamId={} queueBytes={} softMax={}",
                    callbacks.streamId(), queue.bytes(), maxQueueBytes);
            afterEnqueue(enqueuedBytes);
            return;
        }
        ReferenceCountUtil.release(payload);
        logger.error("[限流][下载] 队列触及内存硬顶，丢弃本块但保持连接 streamId={} dropped={} queueBytes={}",
                callbacks.streamId(), enqueuedBytes, queue.bytes());
        callbacks.pauseRemoteDownload();
        scheduleDrain(limiter.scheduleWaitMs(TrafficDirection.DOWNLOAD));
    }

    private void afterEnqueue(int enqueuedBytes) {
        logger.debug("[限流][下载] 入队 streamId={} bytes={} queueBytes={}",
                callbacks.streamId(), enqueuedBytes, queue.bytes());
        callbacks.pauseRemoteDownload();
        scheduleDrain(limiter.scheduleWaitMs(TrafficDirection.DOWNLOAD));
    }

    void drain() {
        drainScheduled.set(false);
        int currentEpoch = epoch.get();
        if (callbacks.isAborted()) {
            queue.clear();
            callbacks.resumeRemoteDownload();
            return;
        }
        while (!queue.isEmpty()) {
            ByteBuf front = queue.peek();
            if (front == null || !front.isReadable()) {
                queue.pollSlice(Integer.MAX_VALUE);
                continue;
            }
            int allowed = limiter.consumeUpTo(TrafficDirection.DOWNLOAD, front.readableBytes());
            if (allowed <= 0) {
                callbacks.pauseRemoteDownload();
                scheduleDrain(limiter.scheduleWaitMs(TrafficDirection.DOWNLOAD));
                return;
            }
            ByteBuf slice = queue.pollSlice(allowed);
            if (slice != null) {
                if (callbacks.isAborted()) {
                    ReferenceCountUtil.release(slice);
                    queue.clear();
                    return;
                }
                logger.debug("[限流][下载] 放行 streamId={} bytes={} queueBytes={}",
                        callbacks.streamId(), slice.readableBytes(), queue.bytes());
                callbacks.forwardDownload(slice, false);
            }
            long queued = queue.bytes();
            if (queued <= lowWaterBytes) {
                callbacks.resumeRemoteDownload();
            } else if (queued >= highWaterBytes) {
                callbacks.pauseRemoteDownload();
            }
        }
        if (currentEpoch != epoch.get()) {
            return;
        }
        logger.debug("[限流][下载] 队列排空，恢复远端 streamId={}", callbacks.streamId());
        callbacks.resumeRemoteDownload();
    }

    void clear() {
        epoch.incrementAndGet();
        queue.clear();
        drainScheduled.set(false);
        callbacks.resumeRemoteDownload();
    }

    private void scheduleDrain(long waitMs) {
        EventLoop loop = callbacks.eventLoop();
        if (loop == null || loop.isShuttingDown()) {
            logger.warn("[限流][下载] 无法调度 drain streamId={} loop={}",
                    callbacks.streamId(), loop);
            return;
        }
        if (!drainScheduled.compareAndSet(false, true)) {
            return;
        }
        int scheduledEpoch = epoch.get();
        loop.schedule(() -> {
            if (scheduledEpoch != epoch.get()) {
                drainScheduled.set(false);
                return;
            }
            try {
                drain();
            } catch (Throwable t) {
                drainScheduled.set(false);
                logger.error("[限流][下载] drain 异常 streamId={}", callbacks.streamId(), t);
                if (!queue.isEmpty() && scheduledEpoch == epoch.get()) {
                    scheduleDrain(Math.max(1, waitMs));
                }
            }
        }, Math.max(1, waitMs), TimeUnit.MILLISECONDS);
    }

    interface DownloadCallbacks {
        int streamId();

        EventLoop eventLoop();

        boolean isAborted();

        void forwardDownload(ByteBuf payload, boolean sharedWithInbound);

        void pauseRemoteDownload();

        void resumeRemoteDownload();
    }
}
