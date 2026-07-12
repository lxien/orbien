package io.github.lxien.orbien.core.transport;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.util.ReferenceCountUtil;

public final class ChannelBufWriter {

    private ChannelBufWriter() {
    }

    public static void write(Channel channel, ByteBuf buf, boolean sharedWithInbound) {
        if (buf == null) {
            return;
        }
        if (!buf.isReadable()) {
            ReferenceCountUtil.release(buf);
            return;
        }
        if (sharedWithInbound) {
            buf.retain();
        }
        channel.writeAndFlush(buf).addListener((ChannelFutureListener) future -> {
            if (!sharedWithInbound) {
                return;
            }
            if (!future.isSuccess()) {
                ReferenceCountUtil.release(buf);
            }
        });
    }
}
