package io.github.lxien.orbien.server.statemachine.stream.action;

import io.github.lxien.orbien.core.codec.NewStreamCodec;
import io.github.lxien.orbien.core.domain.ProxyConfig;
import io.github.lxien.orbien.core.domain.Target;
import io.github.lxien.orbien.core.message.TMSP;
import io.github.lxien.orbien.core.message.TMSPFrame;
import io.github.lxien.orbien.server.statemachine.agent.AgentContext;
import io.github.lxien.orbien.server.statemachine.stream.StreamEvent;
import io.github.lxien.orbien.server.statemachine.stream.StreamState;
import io.github.lxien.orbien.server.statemachine.stream.StreamContext;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class StreamOpenAction extends StreamBaseAction {
    private final InternalLogger logger = InternalLoggerFactory.getInstance(StreamOpenAction.class);

    @Override
    protected void doExecute(StreamState from, StreamState to, StreamEvent event, StreamContext context) {
        int streamId = context.getStreamId();
        AgentContext agentContext = context.getAgentContext();
        Channel control = agentContext.getControl();
        ProxyConfig config = context.getProxyConfig();
        if (!control.isActive()) {
            logger.warn("客户端 {} 控制通道未激活，关闭流 streamId={}", agentContext.getAgentInfo().getAgentId(), streamId);
            context.fireEvent(StreamEvent.STREAM_LOCAL_CLOSE);
            return;
        }
        Target target = context.getTarget();
        ByteBuf payload = control.alloc().buffer();
        NewStreamCodec.encode(payload, target.getHost(), target.getPort());
        TMSPFrame frame = new TMSPFrame(streamId, TMSP.MSG_STREAM_OPEN, payload);

        frame.setMultiplexTunnel(config.isMuxTunnel() || config.isUdp());
        frame.setCompressed(context.isCompress());
        frame.setEncrypted(config.isEncrypt());
        frame.setDatagram(config.isUdp());

        control.writeAndFlush(frame).addListener((ChannelFutureListener) future -> {
            logger.debug("打开流请求引用计数：{}", payload.refCnt());
            if (!future.isSuccess()) {
                logger.error("打开流消息发送失败，关闭流：streamId={} error={}", streamId, future.cause().getMessage());
                context.fireEvent(StreamEvent.STREAM_LOCAL_CLOSE);
            } else {
                control.eventLoop().schedule(() -> {
                    if (context.getState() == StreamState.OPENING) {
                        logger.warn("打开流超时，关闭流 stream={}", streamId);
                        context.fireEvent(StreamEvent.STREAM_LOCAL_CLOSE);
                    }
                }, 10, TimeUnit.SECONDS); // 如果10秒状态未改变，超时处理
                logger.debug("流 {} 打开请求发送成功 代理名: {} 访问目标：{}", streamId, config.getName(), target);
            }
        });
    }
}
