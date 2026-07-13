package io.github.lxien.orbien.core.transport;

import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 多原因 bitmask，全部清除后才恢复 AUTO_READ
 * 用于 visitor / backend 读暂停，避免某一路恢复覆盖另一路暂停
 */
public final class ChannelReadGate {

    public static final int PAUSE_RATE_LIMIT = 1;
    public static final int PAUSE_BACKPRESSURE = 1 << 1;
    public static final int PAUSE_REMOTE = 1 << 2;
    public static final int PAUSE_OPENING = 1 << 3;

    private final AtomicInteger pauseReasons = new AtomicInteger();

    public void pause(Channel channel, int reason) {
        if (channel == null || !channel.isActive()) {
            return;
        }
        int reasons = pauseReasons.updateAndGet(current -> current | reason);
        if (reasons != 0) {
            channel.config().setOption(ChannelOption.AUTO_READ, false);
        }
    }

    public void resume(Channel channel, int reason, boolean canRead) {
        if (channel == null || !channel.isActive()) {
            return;
        }
        int reasons = pauseReasons.updateAndGet(current -> current & ~reason);
        if (reasons == 0 && canRead) {
            channel.config().setOption(ChannelOption.AUTO_READ, true);
            channel.read();
        }
    }

    public boolean isPaused() {
        return pauseReasons.get() != 0;
    }

    public boolean hasReason(int reason) {
        return (pauseReasons.get() & reason) != 0;
    }

    public void reset() {
        pauseReasons.set(0);
    }
}
