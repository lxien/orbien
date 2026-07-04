package io.github.lxien.orbien.client.statemachine.stream;

import com.alibaba.cola.statemachine.StateMachine;
import io.github.lxien.orbien.client.statemachine.agent.AgentContext;
import io.github.lxien.orbien.core.transport.AttributeKeys;
import io.github.lxien.orbien.core.transport.PausedStreamRegistry;
import io.netty.channel.Channel;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class StreamManager {
    private static final Map<Integer, StreamContext> streams = new ConcurrentHashMap<>(1024);
    private static final PausedStreamRegistry pausedStreamRegistry = new PausedStreamRegistry();

    public static StreamContext createStreamContext(Integer streamId, AgentContext agentContext) {
        return streams.computeIfAbsent(streamId, id -> {
            StateMachine<StreamState, StreamEvent, StreamContext> stateMachine =
                    StreamStateMachineBuilder.getStateMachine();
            return new StreamContext(id, stateMachine, agentContext);
        });
    }

    public static Optional<StreamContext> getStreamContext(int streamId) {
        return Optional.ofNullable(streams.get(streamId));
    }

    public static Optional<StreamContext> getStreamContext(Channel server) {
        Integer streamId = server.attr(AttributeKeys.STREAM_ID).get();
        if (streamId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(streams.get(streamId));
    }

    public static boolean removeStreamContext(int streamId) {
        return streams.remove(streamId) != null;
    }

    public static void addPausedStreamId(Channel tunnel, int streamId) {
        pausedStreamRegistry.addPausedStreamId(tunnel, streamId);
    }

    public static   Set<Integer> getPausedStreamIds(Channel tunnel) {
        return pausedStreamRegistry.getPausedStreamIds(tunnel);
    }

    public static void removePausedStream(Channel tunnel, int streamId) {
        pausedStreamRegistry.removePausedStream(tunnel, streamId);
    }
    public static Collection<StreamContext> getStreamContexts(){
       return streams.values();
    }
}
