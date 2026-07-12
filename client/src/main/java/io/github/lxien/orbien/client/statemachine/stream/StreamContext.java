package io.github.lxien.orbien.client.statemachine.stream;

import com.alibaba.cola.statemachine.StateMachine;
import io.github.lxien.orbien.client.statemachine.agent.AgentContext;
import io.github.lxien.orbien.core.transport.AbstractStreamContext;
import io.netty.channel.Channel;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Getter
@Setter
public class StreamContext extends AbstractStreamContext {
    private final InternalLogger logger = InternalLoggerFactory.getInstance(StreamContext.class);
    private StreamState state = StreamState.IDLE;
    private Channel server;
    private String localIp;
    private int localPort;
    private StreamManager streamManager;
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private StateMachine<StreamState, StreamEvent, StreamContext> stateMachine;

    private final AtomicInteger pendingTunnelWrites = new AtomicInteger(0);
    private final AtomicBoolean backendDisconnected = new AtomicBoolean(false);

    public StreamContext(Integer streamId, StateMachine<StreamState, StreamEvent, StreamContext> stateMachine, AgentContext agentContext) {
        this.streamId = streamId;
        this.stateMachine = stateMachine;
        this.agentContext = agentContext;
    }

    public Channel getControl() {
        return agentContext.getControl();
    }

    public void fireEvent(StreamEvent event) {
        stateMachine.fireEvent(state, event, this);
    }

    public void beforeTunnelWrite() {
        pendingTunnelWrites.incrementAndGet();
    }

    public void afterTunnelWrite() {
        if (pendingTunnelWrites.decrementAndGet() < 0) {
            pendingTunnelWrites.set(0);
        }
        tryCloseAfterBackendDisconnected();
    }

    public void markBackendDisconnected() {
        if (backendDisconnected.compareAndSet(false, true)) {
            tryCloseAfterBackendDisconnected();
        }
    }

    private void tryCloseAfterBackendDisconnected() {
        if (!backendDisconnected.get() || pendingTunnelWrites.get() > 0) {
            return;
        }
        if (state == StreamState.CLOSED || state == StreamState.FAILED) {
            return;
        }
        Channel control = getControl();
        Runnable closeTask = () -> {
            if (state != StreamState.CLOSED && state != StreamState.FAILED) {
                fireEvent(StreamEvent.STREAM_LOCAL_CLOSE);
            }
        };
        if (control != null && control.eventLoop() != null) {
            control.eventLoop().execute(closeTask);
        } else {
            closeTask.run();
        }
    }
}
