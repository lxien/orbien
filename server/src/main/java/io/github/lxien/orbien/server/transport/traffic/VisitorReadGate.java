package io.github.lxien.orbien.server.transport.traffic;

import io.github.lxien.orbien.core.transport.ChannelReadGate;

/**
 * Visitor 读门控，语义同 {@link ChannelReadGate}。
 */
public final class VisitorReadGate {

    public static final int PAUSE_RATE_LIMIT = ChannelReadGate.PAUSE_RATE_LIMIT;
    public static final int PAUSE_BACKPRESSURE = ChannelReadGate.PAUSE_BACKPRESSURE;
    public static final int PAUSE_REMOTE = ChannelReadGate.PAUSE_REMOTE;
    public static final int PAUSE_OPENING = ChannelReadGate.PAUSE_OPENING;

    private final ChannelReadGate gate = new ChannelReadGate();

    public void pause(io.netty.channel.Channel visitor, int reason) {
        gate.pause(visitor, reason);
    }

    public void resume(io.netty.channel.Channel visitor, int reason, boolean canRead) {
        gate.resume(visitor, reason, canRead);
    }

    public void reset() {
        gate.reset();
    }
}
