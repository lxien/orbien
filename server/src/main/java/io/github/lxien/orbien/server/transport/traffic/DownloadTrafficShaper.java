package io.github.lxien.orbien.server.transport.traffic;

import io.netty.buffer.ByteBuf;
import io.netty.channel.EventLoop;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 访客下载方向整形。
 * <p>有排队即 pause 远端；队列清空后仍延迟到下一令牌窗口再 resume，
 * 避免 WebSocket 上 PAUSE/RESUME 抖动把 TLS 出站缓冲灌爆。
 */
final class DownloadTrafficShaper {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(DownloadTrafficShaper.class);
    private static final int MAX_QUEUE_FRAMES = 64;
    private static final long MAX_QUEUE_BYTES = 512 * 1024L;
    /**
     * 至少等待一个令牌窗口再 RESUME，避免 nanosToWait(1)==0 时立刻放开灌爆 WS
     */
    private static final long MIN_RESUME_HOLD_MS = 50;

    private final BandwidthLimiter limiter;
    private final TrafficBufferQueue queue = new TrafficBufferQueue();
    private final AtomicBoolean drainScheduled = new AtomicBoolean();
    private final AtomicBoolean resumeScheduled = new AtomicBoolean();
    private final DownloadCallbacks callbacks;

    DownloadTrafficShaper(BandwidthLimiter limiter, DownloadCallbacks callbacks) {
        this.limiter = limiter;
        this.callbacks = callbacks;
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
        if (!queue.offer(queued, MAX_QUEUE_FRAMES, MAX_QUEUE_BYTES)) {
            ReferenceCountUtil.release(queued);
            logger.warn("[限流][下载] 队列溢出 streamId={} queueBytes={}",
                    callbacks.streamId(), queue.bytes());
            callbacks.onDownloadRejected();
            return;
        }
        logger.debug("[限流][下载] 入队 streamId={} bytes={} queueBytes={}",
                callbacks.streamId(), remainderBytes, queue.bytes());
        callbacks.pauseRemoteDownload();
        scheduleDrain(limiter.scheduleWaitMs(TrafficDirection.DOWNLOAD));
    }

    void drain() {
        drainScheduled.set(false);
        if (callbacks.isAborted()) {
            queue.clear();
            resumeScheduled.set(false);
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
        }
        // 队列已空：保持 PAUSE 到下一令牌窗口，再决定是否 RESUME
        callbacks.pauseRemoteDownload();
        scheduleDeferredResume();
    }

    void clear() {
        queue.clear();
        drainScheduled.set(false);
        resumeScheduled.set(false);
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
        loop.schedule(() -> {
            try {
                drain();
            } catch (Throwable t) {
                drainScheduled.set(false);
                logger.error("[限流][下载] drain 异常 streamId={}", callbacks.streamId(), t);
                if (!queue.isEmpty()) {
                    scheduleDrain(Math.max(1, waitMs));
                }
            }
        }, Math.max(1, waitMs), TimeUnit.MILLISECONDS);
    }

    /**
     * 队列排空后延迟 RESUME，等令牌回补，避免立刻放开导致客户端在 WS+TLS 上突发灌缓冲
     */
    private void scheduleDeferredResume() {
        EventLoop loop = callbacks.eventLoop();
        if (loop == null || loop.isShuttingDown()) {
            callbacks.resumeRemoteDownload();
            return;
        }
        if (!resumeScheduled.compareAndSet(false, true)) {
            return;
        }
        long waitMs = Math.max(MIN_RESUME_HOLD_MS, limiter.scheduleWaitMs(TrafficDirection.DOWNLOAD));
        loop.schedule(() -> {
            resumeScheduled.set(false);
            if (callbacks.isAborted()) {
                callbacks.resumeRemoteDownload();
                return;
            }
            if (!queue.isEmpty()) {
                scheduleDrain(1);
                return;
            }
            logger.debug("[限流][下载] 延迟恢复远端 streamId={} holdMs={}",
                    callbacks.streamId(), waitMs);
            callbacks.resumeRemoteDownload();
        }, waitMs, TimeUnit.MILLISECONDS);
    }

    interface DownloadCallbacks {
        int streamId();

        EventLoop eventLoop();

        boolean isAborted();

        void forwardDownload(ByteBuf payload, boolean sharedWithInbound);

        void pauseRemoteDownload();

        void resumeRemoteDownload();

        void onDownloadRejected();
    }
}
