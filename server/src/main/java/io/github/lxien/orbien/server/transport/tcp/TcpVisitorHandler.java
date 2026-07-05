package io.github.lxien.orbien.server.transport.tcp;

import com.alibaba.cola.statemachine.StateMachine;
import io.github.lxien.orbien.core.enums.ProtocolType;
import io.github.lxien.orbien.core.transport.TunnelEntry;
import io.github.lxien.orbien.core.utils.ChannelUtils;
import io.github.lxien.orbien.server.statemachine.stream.StreamContext;
import io.github.lxien.orbien.server.statemachine.stream.StreamEvent;
import io.github.lxien.orbien.server.statemachine.stream.StreamManager;
import io.github.lxien.orbien.server.statemachine.stream.StreamState;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * 处理来自公网访问者的请求
 */
@Component
@ChannelHandler.Sharable
public class TcpVisitorHandler extends SimpleChannelInboundHandler<ByteBuf> {
    private final InternalLogger logger = InternalLoggerFactory.getInstance(TcpVisitorHandler.class);
    @Autowired
    private StreamManager streamManager;
    @Autowired
    @Qualifier("streamStateMachine")
    private StateMachine<StreamState, StreamEvent, StreamContext> stateMachine;

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        logger.debug("[TCP]收到访问者请求");
        Channel visitor = ctx.channel();
        StreamContext streamContext = streamManager.createStreamContext(visitor, stateMachine);
        streamContext.setProtocol(ProtocolType.TCP);
        streamContext.setStreamManager(streamManager);

        streamContext.fireEvent(StreamEvent.STREAM_OPEN);
        ctx.fireChannelActive();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) {
        Channel visitor = ctx.channel();
        Optional<StreamContext> contextOpt = streamManager.getStreamContext(visitor);
        if (contextOpt.isPresent()) {
            StreamContext streamContext = contextOpt.get();
            if (!streamContext.canAcceptVisitorData()) {
                return;
            }
            TunnelEntry tunnelEntry = streamContext.getTunnelEntry();
            if (tunnelEntry == null) {
                logger.error("隧道连接不存在，关闭流：streamId={}", streamContext.getStreamId());
                streamContext.fireEvent(StreamEvent.STREAM_LOCAL_CLOSE);
                return;
            }
            if (streamContext.getTunnelBridge() == null) {
                return;
            }
            Channel tunnel = tunnelEntry.getChannel();
            if (!tunnel.isWritable()) {
                logger.debug("数据无法转发到内网，流量过高，隧道不可写，暂停访问者读取");
                visitor.config().setOption(ChannelOption.AUTO_READ, false);
                streamManager.addPausedStreamId(tunnel, streamContext.getStreamId());
            }
            logger.debug("[TCP] 流 {} 引用计数为：{}", streamContext.getStreamId(), msg.refCnt());
            streamContext.forwardToLocal(msg);
        } else {
            logger.debug("没有找到流上下文，关闭连接");
            ChannelUtils.closeOnFlush(visitor);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        logger.error("[TCP]流异常",cause);
        ctx.fireExceptionCaught(cause);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        logger.debug("[TCP]流断开");
        streamManager.getStreamContext(ctx.channel()).ifPresent(context -> {
            context.fireEvent(StreamEvent.STREAM_LOCAL_CLOSE);
        });
        ctx.fireChannelInactive();
    }
}
