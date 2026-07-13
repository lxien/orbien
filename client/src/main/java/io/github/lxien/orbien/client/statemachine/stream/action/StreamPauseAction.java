package io.github.lxien.orbien.client.statemachine.stream.action;

import io.github.lxien.orbien.client.statemachine.stream.StreamContext;
import io.github.lxien.orbien.client.statemachine.stream.StreamEvent;
import io.github.lxien.orbien.client.statemachine.stream.StreamState;
import io.github.lxien.orbien.core.message.TMSP;
import io.github.lxien.orbien.core.message.TMSPFrame;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

public class StreamPauseAction extends StreamBaseAction {
    private final InternalLogger logger = InternalLoggerFactory.getInstance(StreamPauseAction.class);

    @Override
    protected void doExecute(StreamState from, StreamState to, StreamEvent event, StreamContext context) {
        if (event == StreamEvent.STREAM_REMOTE_PAUSE) {
            // 兼容旧路径 主路径已在 ControlFrameHandler 直接调用 pauseBackendRead
            logger.debug("暂停本地服务流读取 streamId={}", context.getStreamId());
            context.pauseBackendRead(StreamContext.BACKEND_PAUSE_RATE_LIMIT);
        }
        if (event == StreamEvent.STREAM_LOCAL_PAUSE) {
            sendPauseToRemote(context);
        }
    }

    private void sendPauseToRemote(StreamContext context) {
        logger.debug("通知远程代理服务器暂停流 {} 读取", context.getStreamId());
        TMSPFrame frame = new TMSPFrame(context.getStreamId(), TMSP.MSG_STREAM_PAUSE);
        context.getControl().writeAndFlush(frame);
    }
}
