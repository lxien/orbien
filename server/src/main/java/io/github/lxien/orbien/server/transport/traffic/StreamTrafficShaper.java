package io.github.lxien.orbien.server.transport.traffic;

import io.github.lxien.orbien.server.statemachine.stream.StreamContext;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.EventLoop;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

/**
 * 单流带宽整形入口
 * 下载数据来自隧道 EventLoop，必须投递到 visitor EventLoop 再整形
 */
public final class StreamTrafficShaper {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(StreamTrafficShaper.class);

    private final UploadTrafficShaper uploadShaper;
    private final DownloadTrafficShaper downloadShaper;
    private final StreamContext context;

    public StreamTrafficShaper(BandwidthLimiter limiter, StreamContext context, VisitorReadGate readGate) {
        this.context = context;
        this.uploadShaper = new UploadTrafficShaper(limiter, new UploadTrafficShaper.UploadCallbacks() {
            @Override
            public int streamId() {
                return context.getStreamId();
            }

            @Override
            public EventLoop eventLoop() {
                return visitorEventLoop();
            }

            @Override
            public boolean isAborted() {
                return context.isLocalForwardingAborted();
            }

            @Override
            public void forwardUpload(ByteBuf payload, boolean sharedWithInbound) {
                context.forwardToLocalUnchecked(payload, sharedWithInbound);
            }

            @Override
            public void pauseVisitorRead() {
                readGate.pause(context.getVisitor(), VisitorReadGate.PAUSE_RATE_LIMIT);
            }

            @Override
            public void resumeVisitorRead() {
                readGate.resume(context.getVisitor(), VisitorReadGate.PAUSE_RATE_LIMIT,
                        context.canResumeVisitorRead());
            }
        });
        this.downloadShaper = new DownloadTrafficShaper(limiter, new DownloadTrafficShaper.DownloadCallbacks() {
            @Override
            public int streamId() {
                return context.getStreamId();
            }

            @Override
            public EventLoop eventLoop() {
                return visitorEventLoop();
            }

            @Override
            public boolean isAborted() {
                return context.isLocalForwardingAborted();
            }

            @Override
            public void forwardDownload(ByteBuf payload, boolean sharedWithInbound) {
                context.forwardToRemoteUnchecked(payload, sharedWithInbound);
            }

            @Override
            public void pauseRemoteDownload() {
                context.pauseRemoteProducer(StreamContext.REMOTE_PAUSE_RATE_LIMIT);
            }

            @Override
            public void resumeRemoteDownload() {
                context.resumeRemoteProducer(StreamContext.REMOTE_PAUSE_RATE_LIMIT);
            }
        });
    }

    public boolean tryUpload(ByteBuf payload, boolean sharedWithInbound) {
        return uploadShaper.offer(payload, sharedWithInbound);
    }

    /**
     * @return false 表示已接管；true 表示无整形，调用方继续转发
     */
    public boolean tryDownload(ByteBuf payload, boolean sharedWithInbound) {
        Channel visitor = context.getVisitor();
        if (visitor == null || !visitor.isActive()) {
            return true;
        }
        EventLoop loop = visitor.eventLoop();
        if (sharedWithInbound) {
            payload.retain();
        }
        if (loop.inEventLoop()) {
            downloadShaper.offerOwned(payload);
        } else {
            loop.execute(() -> {
                try {
                    downloadShaper.offerOwned(payload);
                } catch (Throwable t) {
                    ReferenceCountUtil.release(payload);
                    logger.error("[限流][下载] offer 异常 streamId={}", context.getStreamId(), t);
                }
            });
        }
        return false;
    }

    public void clear() {
        EventLoop loop = visitorEventLoop();
        if (loop == null || loop.inEventLoop()) {
            uploadShaper.clear();
            downloadShaper.clear();
            return;
        }
        loop.execute(() -> {
            uploadShaper.clear();
            downloadShaper.clear();
        });
    }

    private EventLoop visitorEventLoop() {
        Channel visitor = context.getVisitor();
        return visitor != null ? visitor.eventLoop() : null;
    }
}
