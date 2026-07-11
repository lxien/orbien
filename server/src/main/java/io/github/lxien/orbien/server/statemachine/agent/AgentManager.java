package io.github.lxien.orbien.server.statemachine.agent;

import com.alibaba.cola.statemachine.StateMachine;
import io.github.lxien.orbien.core.transport.AttributeKeys;
import io.github.lxien.orbien.server.generator.ConnectionIdGenerator;
import io.netty.channel.Channel;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Component
public class AgentManager {
    private final InternalLogger logger = InternalLoggerFactory.getInstance(AgentManager.class);
    private final Map<Integer/*connectionId*/, AgentContext> connectionToContextMap = new ConcurrentHashMap<>();
    private final Map<String/*agentId*/, AgentContext> agentToContextMap = new ConcurrentHashMap<>();
    private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();
    private final Lock writeLock = rwLock.writeLock();
    @Autowired
    private ConnectionIdGenerator connectionIdGenerator;

    public Optional<AgentContext> getAgentContext(Channel control) {
        Integer connectionId = control.attr(AttributeKeys.CONNECTION_ID).get();
        if (connectionId == null) {
            return Optional.empty();
        }
        AgentContext agentContext = connectionToContextMap.get(connectionId);
        return Optional.ofNullable(agentContext);
    }

    public Optional<AgentContext> getAgentContext(String agentId) {
        return Optional.ofNullable(agentToContextMap.get(agentId));
    }

    public Optional<AgentContext> getAgentContext(Integer connectionId) {
        return Optional.ofNullable(connectionToContextMap.get(connectionId));
    }

    public AgentContext createAgent(Channel control, StateMachine<AgentState, AgentEvent, AgentContext> agentStateMachine) {
        int connectionId = connectionIdGenerator.nextConnId();
        AgentContext agentContext = new AgentContext(agentStateMachine);
        agentContext.setControl(control);
        control.attr(AttributeKeys.CONNECTION_ID).set(connectionId);
        agentContext.setConnectionId(connectionId);
        connectionToContextMap.put(connectionId, agentContext);
        return agentContext;
    }
    public void addAgentContextIndex(String agentId, AgentContext context) {
        agentToContextMap.put(agentId, context);
    }

    /**
     * 断线重连时，将已有会话绑定到新的控制连接。
     */
    public void rebindControlChannel(AgentContext context, Channel newChannel) {
        writeLock.lock();
        try {
            Integer oldConnectionId = context.getConnectionId();
            if (oldConnectionId != null) {
                connectionToContextMap.remove(oldConnectionId);
            }
            int connectionId = connectionIdGenerator.nextConnId();
            newChannel.attr(AttributeKeys.CONNECTION_ID).set(connectionId);
            context.setConnectionId(connectionId);
            context.setControl(newChannel);
            connectionToContextMap.put(connectionId, context);
        } finally {
            writeLock.unlock();
        }
    }

    public int getOnlineCount() {
        return connectionToContextMap.size();
    }

    public void removeAgentContext(String agentId) {
        writeLock.lock();
        try {
            AgentContext agentContext = agentToContextMap.remove(agentId);
            if (agentContext != null) {
                Integer connectionId = agentContext.getConnectionId();
                connectionToContextMap.remove(connectionId);
            }
        } finally {
            writeLock.unlock();
        }
    }
    public boolean isOnline(String agentId) {
        AgentContext agentContext = agentToContextMap.get(agentId);
        if (agentContext == null) {
            return false;
        }
        AgentState state = agentContext.getState();
        return state == AgentState.CONNECTED;
    }


    public void kickout(String agentId) {
        AgentContext agentContext = agentToContextMap.get(agentId);
        if (agentContext == null) {
            logger.debug("强制下线跳过，客户端不在线: {}", agentId);
            return;
        }
        if (agentContext.getState() != AgentState.CONNECTED) {
            logger.debug("强制下线跳过，客户端状态非 CONNECTED: {} state={}", agentId, agentContext.getState());
            return;
        }
        Channel control = agentContext.getControl();
        if (control != null && control.isActive()) {
            control.eventLoop().execute(() -> agentContext.fireEvent(AgentEvent.LOCAL_GOAWAY));
        } else {
            agentContext.fireEvent(AgentEvent.LOCAL_GOAWAY);
        }
    }

    public Collection<AgentContext> getAllAgentContext() {
        return agentToContextMap.values();
    }

}
