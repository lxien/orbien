package io.github.lxien.orbien.core.transport;

import io.github.lxien.orbien.core.utils.ChannelUtils;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * 空闲检查。
 * <p>数据隧道在限流/背压暂停写时不应因 writer idle 被拆掉，
 */
public class IdleCheckHandler extends IdleStateHandler {
    private final InternalLogger logger = InternalLoggerFactory.getInstance(IdleCheckHandler.class);

    public IdleCheckHandler() {
        super(300, 180, 0, TimeUnit.SECONDS);
    }

    public IdleCheckHandler(long readerIdleSeconds, long writerIdleSeconds) {
        super(readerIdleSeconds, writerIdleSeconds, 0, TimeUnit.SECONDS);
    }

    /**
     * 只检查读空闲，关闭写空闲（限流 PAUSE 期间可能长时间不写）
     */
    public static IdleCheckHandler forDataTunnel() {
        return new IdleCheckHandler(300, 0);
    }

    @Override
    protected void channelIdle(ChannelHandlerContext ctx, IdleStateEvent evt) {
        Channel channel = ctx.channel();
        switch (evt.state()) {
            case WRITER_IDLE:
                logger.debug("写超时，关闭连接 {}", channel.remoteAddress());
                ChannelUtils.closeOnFlush(channel);
                break;

            case READER_IDLE:
                logger.debug("读空闲超时，关闭连接 {}", channel.remoteAddress());
                ChannelUtils.closeOnFlush(channel);
                break;
            default:
                break;
        }
        ctx.fireUserEventTriggered(evt);
    }
}
