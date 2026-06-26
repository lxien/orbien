package com.xiaoniucode.etp.server.transport.http;

import com.alibaba.cola.statemachine.StateMachine;
import com.xiaoniucode.etp.core.transport.AttributeKeys;
import com.xiaoniucode.etp.core.enums.ProtocolType;
import com.xiaoniucode.etp.core.transport.TunnelEntry;
import com.xiaoniucode.etp.core.utils.ChannelUtils;
import com.xiaoniucode.etp.server.statemachine.stream.StreamEvent;
import com.xiaoniucode.etp.server.statemachine.stream.StreamContext;
import com.xiaoniucode.etp.server.statemachine.stream.StreamState;
import com.xiaoniucode.etp.server.statemachine.stream.StreamManager;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.net.ssl.SSLHandshakeException;
import java.util.Optional;

@Component
@ChannelHandler.Sharable
public class HttpVisitorHandler extends SimpleChannelInboundHandler<ByteBuf> {
    private final InternalLogger logger = InternalLoggerFactory.getInstance(HttpVisitorHandler.class);
    @Autowired
    private StreamManager streamManager;
    @Autowired
    @Qualifier("streamStateMachine")
    private StateMachine<StreamState, StreamEvent, StreamContext> stateMachine;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf buf) {
        Channel visitor = ctx.channel();
        Optional<StreamContext> contextOpt = streamManager.getStreamContext(visitor);
        if (contextOpt.isPresent()) {
            StreamContext streamContext = contextOpt.get();
            if (streamContext.getState() == StreamState.OPENED) {
                TunnelEntry tunnelEntry = streamContext.getTunnelEntry();
                Channel tunnel = tunnelEntry.getChannel();
                if (!tunnel.isWritable()) {
                    logger.warn("数据无法转发到内网，流量过高，隧道不可写，暂停访问者读取");
                    visitor.config().setOption(ChannelOption.AUTO_READ, false);
                    if (tunnelEntry.getTunnelType().isMultiplex()) {
                        streamManager.addPausedStreamId(tunnel, streamContext.getStreamId());
                    }
                }
                streamContext.forwardToLocal(buf);
            } else {
                logger.error("隧道未开启，无法传输数据");
            }
        } else {
            logger.debug("[HTTP] 创建流上下文");
            visitor.attr(AttributeKeys.HTTP_FIRST_PACKET).set(buf.retain());
            StreamContext streamContext = streamManager.createStreamContext(visitor, stateMachine);
            streamContext.setProtocol(ProtocolType.HTTP);
            streamContext.setStreamManager(streamManager);
            streamContext.fireEvent(StreamEvent.STREAM_OPEN);
        }
    }


    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        Channel visitor = ctx.channel();
        streamManager.getStreamContext(visitor).ifPresent(context -> {
            logger.debug("[HTTP]访问者连接断开，关闭流: streamId={}", context.getStreamId());
            context.fireEvent(StreamEvent.STREAM_LOCAL_CLOSE);
        });
        ctx.fireChannelInactive();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        if (isCertificateUnknownError(cause)) {
            ChannelUtils.closeOnFlush(ctx.channel());
            logger.debug("证书未知错误，可能是客户端未信任自签名证书");
            return;
        }
        logger.error("[HTTP]发生异常", cause);
        streamManager.getStreamContext(ctx.channel()).ifPresent(streamContext -> {
            logger.error("[HTTP] 访问者连接发生异常，关闭流", cause);
            streamContext.fireEvent(StreamEvent.STREAM_LOCAL_CLOSE);
        });
        ctx.fireExceptionCaught(cause);
    }

    /**
     * 证书未知错误异常判断
     *
     * @param cause 异常
     * @return 是否是未知SSL证书异常
     */
    private boolean isCertificateUnknownError(Throwable cause) {
        while (cause != null) {
            if (cause instanceof SSLHandshakeException && cause.getMessage() != null &&
                    cause.getMessage().contains("certificate_unknown")) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }

    @Override
    public void channelWritabilityChanged(ChannelHandlerContext ctx) {
        Channel visitor = ctx.channel();
        logger.warn("[HTTP] 访问者可写性发生变化：{}", visitor.isWritable());
        streamManager.getStreamContext(visitor).ifPresent(streamContext -> {
            logger.warn("流量过高，触发背压");
            if (!visitor.isWritable()) {
                streamContext.fireEvent(StreamEvent.STREAM_LOCAL_PAUSE);
            } else {
                streamContext.fireEvent(StreamEvent.STREAM_LOCAL_RESUME);
            }
        });
        ctx.fireChannelWritabilityChanged();
    }
}
