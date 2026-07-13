package io.github.lxien.orbien.server.transport.traffic;

import io.github.lxien.orbien.core.message.TMSP;
import io.github.lxien.orbien.core.message.TMSPFrame;
import io.netty.channel.Channel;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

/**
 * 远端（Agent 后端）生产者门控：限流排队 / visitor 不可写等原因 bitmask，
 * 首次置位发 PAUSE，全部清除发 RESUME。不经过流状态机。
 */
public final class RemoteProducerGate {

    public static final int REASON_RATE_LIMIT = 1;
    public static final int REASON_VISITOR_BACKPRESSURE = 1 << 1;

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(RemoteProducerGate.class);

    private final AtomicInteger reasons = new AtomicInteger();
    private final IntSupplier streamId;
    private final Supplier<Channel> controlChannel;

    public RemoteProducerGate(IntSupplier streamId, Supplier<Channel> controlChannel) {
        this.streamId = streamId;
        this.controlChannel = controlChannel;
    }

    public void pause(int reason) {
        for (;;) {
            int current = reasons.get();
            int next = current | reason;
            if (!reasons.compareAndSet(current, next)) {
                continue;
            }
            if (current == 0 && next != 0) {
                send(TMSP.MSG_STREAM_PAUSE, "暂停");
            }
            return;
        }
    }

    public void resume(int reason) {
        for (;;) {
            int current = reasons.get();
            if ((current & reason) == 0) {
                return;
            }
            int next = current & ~reason;
            if (!reasons.compareAndSet(current, next)) {
                continue;
            }
            if (next == 0) {
                send(TMSP.MSG_STREAM_RESUME, "恢复");
            }
            return;
        }
    }

    public void reset() {
        reasons.set(0);
    }

    private void send(byte type, String action) {
        Channel control = controlChannel.get();
        if (control == null || !control.isActive()) {
            logger.warn("[背压] 无法{}远端 streamId={}：控制通道不可用", action, streamId.getAsInt());
            return;
        }
        TMSPFrame frame = new TMSPFrame(streamId.getAsInt(), type);
        Runnable write = () -> control.writeAndFlush(frame);
        if (control.eventLoop().inEventLoop()) {
            write.run();
        } else {
            control.eventLoop().execute(write);
        }
        if (logger.isDebugEnabled()) {
            logger.debug("[背压] 已通知远端{} streamId={} reasons={}",
                    action, streamId.getAsInt(), reasons.get());
        }
    }
}
