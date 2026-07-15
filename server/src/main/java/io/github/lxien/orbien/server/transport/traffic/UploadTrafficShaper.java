package io.github.lxien.orbien.server.transport.traffic;

import io.netty.buffer.ByteBuf;
import io.netty.channel.EventLoop;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

final class UploadTrafficShaper {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(UploadTrafficShaper.class);
    private static final long MIN_QUEUE_BYTES = 1 * 1024 * 1024L;
    private static final long MAX_QUEUE_BYTES_CAP = 8 * 1024 * 1024L;
    private static final int MAX_QUEUE_FRAMES = 4096;
    private static final long EMERGENCY_DROP_BYTES = 16 * 1024 * 1024L;

    private final BandwidthLimiter limiter;
    private final TrafficBufferQueue queue = new TrafficBufferQueue();
    private final AtomicBoolean drainScheduled = new AtomicBoolean();
    private final AtomicInteger epoch = new AtomicInteger();
    private final UploadCallbacks callbacks;
    private final long maxQueueBytes;

    UploadTrafficShaper(BandwidthLimiter limiter, UploadCallbacks callbacks) {
        this.limiter = limiter;
        this.callbacks = callbacks;
        long bps = limiter != null ? Math.max(0, limiter.getBytesPerSecond()) : 0;
        this.maxQueueBytes = Math.min(MAX_QUEUE_BYTES_CAP, Math.max(MIN_QUEUE_BYTES, bps * 4));
    }

    /**
     * @return true 表示调用方应继续转发 payload；false 表示 payload 已被接管
     */
    boolean offer(ByteBuf payload, boolean sharedWithInbound) {
        if (payload == null || !payload.isReadable()) {
            return true;
        }
        if (limiter == null || !limiter.isEnabled()) {
            return true;
        }
        EventLoop loop = callbacks.eventLoop();
        if (loop == null) {
            return true;
        }
        if (!loop.inEventLoop()) {
            if (sharedWithInbound) {
                payload.retain();
            }
            loop.execute(() -> offerOwned(payload));
            return false;
        }
        if (sharedWithInbound) {
            payload.retain();
        }
        offerOwned(payload);
        return false;
    }

    private void offerOwned(ByteBuf payload) {
        if (payload == null || !payload.isReadable()) {
            ReferenceCountUtil.release(payload);
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
        int allowed = limiter.consumeUpTo(TrafficDirection.UPLOAD, readable);
        if (allowed >= readable) {
            callbacks.forwardUpload(payload, false);
            return;
        }
        if (allowed > 0) {
            ByteBuf slice = payload.retainedSlice(readerIndex, allowed);
            callbacks.forwardUpload(slice, false);
        }
        if (callbacks.isAborted()) {
            ReferenceCountUtil.release(payload);
            return;
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
        if (queue.offer(payload, MAX_QUEUE_FRAMES, maxQueueBytes)) {
            afterEnqueue(payload.readableBytes());
            return;
        }
        if (queue.bytes() + payload.readableBytes() <= EMERGENCY_DROP_BYTES
                && queue.offer(payload, MAX_QUEUE_FRAMES * 2, EMERGENCY_DROP_BYTES)) {
            logger.warn("[限流][上传] 队列超过软上限，继续背压 streamId={} queueBytes={} softMax={}",
                    callbacks.streamId(), queue.bytes(), maxQueueBytes);
            afterEnqueue(payload.readableBytes());
            return;
        }
        int dropped = payload.readableBytes();
        ReferenceCountUtil.release(payload);
        logger.error("[限流][上传] 队列触及内存硬顶，丢弃本块但保持连接 streamId={} dropped={} queueBytes={}",
                callbacks.streamId(), dropped, queue.bytes());
        callbacks.pauseVisitorRead();
        scheduleDrain(limiter.scheduleWaitMs(TrafficDirection.UPLOAD));
    }

    private void afterEnqueue(int enqueuedBytes) {
        logger.debug("[限流][上传] 入队 streamId={} bytes={} queueBytes={}",
                callbacks.streamId(), enqueuedBytes, queue.bytes());
        callbacks.pauseVisitorRead();
        scheduleDrain(limiter.scheduleWaitMs(TrafficDirection.UPLOAD));
    }

    void drain() {
        drainScheduled.set(false);
        int currentEpoch = epoch.get();
        if (callbacks.isAborted()) {
            queue.clear();
            callbacks.resumeVisitorRead();
            return;
        }
        while (!queue.isEmpty()) {
            ByteBuf front = queue.peek();
            if (front == null || !front.isReadable()) {
                queue.pollSlice(Integer.MAX_VALUE);
                continue;
            }
            int allowed = limiter.consumeUpTo(TrafficDirection.UPLOAD, front.readableBytes());
            if (allowed <= 0) {
                callbacks.pauseVisitorRead();
                scheduleDrain(limiter.scheduleWaitMs(TrafficDirection.UPLOAD));
                return;
            }
            ByteBuf slice = queue.pollSlice(allowed);
            if (slice != null) {
                if (callbacks.isAborted()) {
                    ReferenceCountUtil.release(slice);
                    queue.clear();
                    return;
                }
                logger.debug("[限流][上传] 放行 streamId={} bytes={} queueBytes={}",
                        callbacks.streamId(), slice.readableBytes(), queue.bytes());
                callbacks.forwardUpload(slice, false);
            }
        }
        if (currentEpoch != epoch.get()) {
            return;
        }
        callbacks.resumeVisitorRead();
    }

    void clear() {
        epoch.incrementAndGet();
        queue.clear();
        drainScheduled.set(false);
        callbacks.resumeVisitorRead();
    }

    private void scheduleDrain(long waitMs) {
        EventLoop loop = callbacks.eventLoop();
        if (loop == null || loop.isShuttingDown()) {
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
                logger.error("[限流][上传] drain 异常 streamId={}", callbacks.streamId(), t);
                if (!queue.isEmpty() && scheduledEpoch == epoch.get()) {
                    scheduleDrain(Math.max(1, waitMs));
                }
            }
        }, Math.max(1, waitMs), TimeUnit.MILLISECONDS);
    }

    interface UploadCallbacks {
        int streamId();

        EventLoop eventLoop();

        boolean isAborted();

        void forwardUpload(ByteBuf payload, boolean sharedWithInbound);

        void pauseVisitorRead();

        void resumeVisitorRead();
    }
}
