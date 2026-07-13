package io.github.lxien.orbien.server.transport.traffic;

import io.netty.buffer.ByteBuf;
import io.netty.channel.EventLoop;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 访客上传方向（limit_out）整形
 */
final class UploadTrafficShaper {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(UploadTrafficShaper.class);
    private static final int MAX_QUEUE_FRAMES = 256;
    private static final long MAX_QUEUE_BYTES = 2 * 1024 * 1024L;
    private static final long HARD_REJECT_WAIT_MS = 500;

    private final BandwidthLimiter limiter;
    private final TrafficBufferQueue queue = new TrafficBufferQueue();
    private final AtomicBoolean drainScheduled = new AtomicBoolean();
    private final UploadCallbacks callbacks;

    UploadTrafficShaper(BandwidthLimiter limiter, UploadCallbacks callbacks) {
        this.limiter = limiter;
        this.callbacks = callbacks;
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
        if (callbacks.isAborted()) {
            ReferenceCountUtil.release(queued);
            return;
        }
        if (!queue.offer(queued, MAX_QUEUE_FRAMES, MAX_QUEUE_BYTES)) {
            ReferenceCountUtil.release(queued);
            logger.warn("[限流][上传] 队列溢出 streamId={} queueBytes={}",
                    callbacks.streamId(), queue.bytes());
            callbacks.onUploadRejected();
            return;
        }
        logger.debug("[限流][上传] 入队 streamId={} bytes={} queueBytes={}",
                callbacks.streamId(), remainderBytes, queue.bytes());
        callbacks.pauseVisitorRead();
        scheduleDrain(limiter.scheduleWaitMs(TrafficDirection.UPLOAD));
    }

    void drain() {
        drainScheduled.set(false);
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
                long waitMs = limiter.scheduleWaitMs(TrafficDirection.UPLOAD);
                if (waitMs > HARD_REJECT_WAIT_MS) {
                    logger.warn("[限流][上传] 等待过久，拒绝 streamId={} waitMs={} queueBytes={}",
                            callbacks.streamId(), waitMs, queue.bytes());
                    queue.clear();
                    callbacks.onUploadRejected();
                    return;
                }
                callbacks.pauseVisitorRead();
                scheduleDrain(waitMs);
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
        callbacks.resumeVisitorRead();
    }

    void clear() {
        queue.clear();
        drainScheduled.set(false);
    }

    private void scheduleDrain(long waitMs) {
        EventLoop loop = callbacks.eventLoop();
        if (loop == null || loop.isShuttingDown()) {
            return;
        }
        if (!drainScheduled.compareAndSet(false, true)) {
            return;
        }
        loop.schedule(() -> {
            try {
                drain();
            } catch (Throwable t) {
                drainScheduled.set(false);
                logger.error("[限流][上传] drain 异常 streamId={}", callbacks.streamId(), t);
                if (!queue.isEmpty()) {
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

        void onUploadRejected();
    }
}
