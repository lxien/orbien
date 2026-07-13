package io.github.lxien.orbien.client.statemachine.stream.action;

import io.github.lxien.orbien.client.statemachine.stream.StreamContext;
import io.github.lxien.orbien.client.statemachine.stream.StreamEvent;
import io.github.lxien.orbien.client.statemachine.stream.StreamState;
import io.github.lxien.orbien.core.message.TMSP;
import io.github.lxien.orbien.core.message.TMSPFrame;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

public class StreamResumeAction extends StreamBaseAction {
    private final InternalLogger logger = InternalLoggerFactory.getInstance(StreamResumeAction.class);

    @Override
    protected void doExecute(StreamState from, StreamState to, StreamEvent event, StreamContext context) {
        if (event == StreamEvent.STREAM_REMOTE_RESUME) {
            logger.debug("恢复本地服务流读取 streamId={}", context.getStreamId());
            context.resumeBackendRead(StreamContext.BACKEND_PAUSE_RATE_LIMIT);
        }
        if (event == StreamEvent.STREAM_LOCAL_RESUME) {
            sendResumeToRemote(context);
        }
    }

    private void sendResumeToRemote(StreamContext context) {
        logger.debug("通知远程代理服务器恢复流 {} 读取", context.getStreamId());
        TMSPFrame frame = new TMSPFrame(context.getStreamId(), TMSP.MSG_STREAM_RESUME);
        context.getControl().writeAndFlush(frame);
    }
}
