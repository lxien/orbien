package io.github.lxien.orbien.client.transport;

import io.github.lxien.orbien.client.statemachine.stream.StreamContext;
import io.github.lxien.orbien.client.statemachine.stream.StreamEvent;
import io.github.lxien.orbien.client.statemachine.stream.StreamManager;
import io.github.lxien.orbien.core.transport.TunnelEntry;
import io.github.lxien.orbien.core.utils.ChannelUtils;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.util.Optional;

/**
 *
 * @author liuxin
 */
public class RealServerHandler extends SimpleChannelInboundHandler<ByteBuf> {
    private final InternalLogger logger = InternalLoggerFactory.getInstance(RealServerHandler.class);

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) {
        Channel server = ctx.channel();
        Optional<StreamContext> streamCtx = StreamManager.getStreamContext(server);
        streamCtx.ifPresent(streamContext -> {
                    TunnelEntry tunnelEntry = streamContext.getTunnelEntry();
                    Channel tunnel = tunnelEntry.getChannel();
                    if (!tunnel.isWritable()) {
                        logger.debug("数据无法转发到远程，流量过高，隧道不可写，暂停从服务读取，streamId={}", streamContext.getStreamId());
                        server.config().setOption(ChannelOption.AUTO_READ, false);
                        if (tunnelEntry.getTunnelType().isMultiplex()){
                            StreamManager.addPausedStreamId(tunnel, streamContext.getStreamId());
                        }
                    }
                    streamContext.forwardToRemote(msg);
                }
        );
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        logger.debug("目标服务连接断开",ctx);
        Channel server = ctx.channel();
        Optional<StreamContext> streamCtx = StreamManager.getStreamContext(server);
        streamCtx.ifPresent(streamContext -> {
            logger.debug("目标服务连接断开，关闭流 {} ", streamContext.getStreamId());
            streamContext.fireEvent(StreamEvent.STREAM_LOCAL_CLOSE);
        });
        if (streamCtx.isEmpty()) {
            logger.debug("没有获取到真实服务流信息");
        }
        ctx.fireChannelInactive();
    }

    @Override
    public void channelWritabilityChanged(ChannelHandlerContext ctx) {
        logger.warn("服务端可写状态发生变化，当前状态：{}", ctx.channel().isWritable());
        Channel server = ctx.channel();
        StreamManager.getStreamContext(server).ifPresent(streamContext -> {
            Channel tunnel = streamContext.getTunnelEntry().getChannel();
            if (tunnel != null) {
                logger.warn("隧道流量过高，改变隧道的可读状态，无法写入服务器，当前隧道可写状态：{}", tunnel.isWritable());
                boolean shouldRead = server.isWritable();
                tunnel.config().setOption(ChannelOption.AUTO_READ, shouldRead);
            }
        });
        ctx.fireChannelWritabilityChanged();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        logger.error("目标服务连接发生异常", cause);
        Channel server = ctx.channel();
        Optional<StreamContext> streamCtx = StreamManager.getStreamContext(server);
        streamCtx.ifPresent(streamContext -> {
            logger.error("目标服务连接发生异常，关闭流：{}",streamContext.getStreamId());
            streamContext.fireEvent(StreamEvent.STREAM_LOCAL_CLOSE);
        });
        ChannelUtils.closeOnFlush(server);
        ctx.fireExceptionCaught(cause);
    }

}
