package io.github.lxien.orbien.core.transport;

import io.netty.channel.Channel;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.timeout.IdleStateHandler;

import java.util.concurrent.TimeUnit;

public class PipelineConfigure {

    public static void removeControlHandler(Channel channel) {
        ChannelPipeline pipeline = channel.pipeline();
        String[] handlersToRemove = {
                NettyConstants.TMSP_CODEC,
                NettyConstants.CONTROL_FRAME_HANDLER,
                NettyConstants.CONTROL_IDLE_CHECK_HANDLER
        };
        for (String handlerName : handlersToRemove) {
            if (pipeline.get(handlerName) != null) {
                pipeline.remove(handlerName);
            }
        }
    }

    public static void removeControlIdleCheckHandler(Channel channel) {
        if (channel == null || !channel.isActive()) {
            return;
        }
        ChannelPipeline pipeline = channel.pipeline();
        if (pipeline.get(NettyConstants.CONTROL_IDLE_CHECK_HANDLER) != null) {
            pipeline.remove(NettyConstants.CONTROL_IDLE_CHECK_HANDLER);
        }
    }
}
