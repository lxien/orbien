package io.github.lxien.orbien.server.statemachine.stream.action;

import com.alibaba.cola.statemachine.Action;
import io.github.lxien.orbien.server.statemachine.stream.StreamEvent;
import io.github.lxien.orbien.server.statemachine.stream.StreamState;
import io.github.lxien.orbien.server.statemachine.stream.StreamContext;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

public abstract class StreamBaseAction implements Action<StreamState, StreamEvent, StreamContext> {
private final InternalLogger logger= InternalLoggerFactory.getInstance(StreamBaseAction.class);
    @Override
    public final void execute(StreamState from, StreamState to, StreamEvent event, StreamContext context) {
        context.setState(to);
        try {
            doExecute(from, to, event, context);
        } catch (Exception e) {
            logger.error("执行错误",e);
        }
    }

    protected abstract void doExecute(StreamState from, StreamState to, StreamEvent event, StreamContext context);
}