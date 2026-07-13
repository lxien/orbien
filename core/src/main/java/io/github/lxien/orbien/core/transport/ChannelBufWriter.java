package io.github.lxien.orbien.core.transport;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.util.ReferenceCountUtil;
public final class ChannelBufWriter {

    private ChannelBufWriter() {
    }

    public static void write(Channel channel, ByteBuf buf, boolean sharedWithInbound) {
        write(channel, buf, sharedWithInbound, null);
    }

    public static void write(Channel channel, ByteBuf buf, boolean sharedWithInbound,
                             ChannelFutureListener afterWrite) {
        if (buf == null) {
            return;
        }
        if (!buf.isReadable()) {
            if (!sharedWithInbound) {
                ReferenceCountUtil.release(buf);
            }
            return;
        }
        if (channel == null || !channel.isActive()) {
            if (!sharedWithInbound) {
                ReferenceCountUtil.release(buf);
            }
            return;
        }
        final ByteBuf outbound = sharedWithInbound ? buf.retain() : buf;
        channel.writeAndFlush(outbound).addListener((ChannelFutureListener) future -> {
            if (afterWrite != null) {
                afterWrite.operationComplete(future);
            }
        });
    }
}
