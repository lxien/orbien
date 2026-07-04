package io.github.lxien.orbien.client.statemachine.stream.action;

import com.alibaba.cola.statemachine.Action;
import io.github.lxien.orbien.client.statemachine.stream.StreamContext;
import io.github.lxien.orbien.client.statemachine.stream.StreamEvent;
import io.github.lxien.orbien.client.statemachine.stream.StreamState;

public abstract class StreamBaseAction implements Action<StreamState, StreamEvent, StreamContext> {
    @Override
    public void execute(StreamState from, StreamState to, StreamEvent event, StreamContext context) {
        context.setState(to);
        doExecute(from, to, event, context);
    }
    protected abstract void doExecute(StreamState from, StreamState to, StreamEvent event, StreamContext context);
}
