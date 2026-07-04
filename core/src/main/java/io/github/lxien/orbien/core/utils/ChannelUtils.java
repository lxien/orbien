package io.github.lxien.orbien.core.utils;

import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;

public final class ChannelUtils {

    public static void closeOnFlush(Channel channel) {
        if (channel != null && channel.isActive()) {
            channel.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
        }
    }

    private ChannelUtils() {
    }
}
