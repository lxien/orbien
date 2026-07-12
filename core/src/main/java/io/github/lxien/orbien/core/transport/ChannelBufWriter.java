package io.github.lxien.orbien.core.transport;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.util.ReferenceCountUtil;

/**
 * 统一 Channel 写 ByteBuf 的引用计数策略。
 */
public final class ChannelBufWriter {

    private ChannelBufWriter() {
    }

    /**
     * 将 ByteBuf 写入 Channel。
     *
     * @param sharedWithInbound true 表示 buf 与 inbound 消息（如 TMSPFrame payload）共享生命周期，
     *                          需要 retain 以抵消 inbound handler 随后的 release
     */
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
