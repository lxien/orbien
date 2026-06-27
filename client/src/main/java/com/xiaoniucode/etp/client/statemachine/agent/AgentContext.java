package com.xiaoniucode.etp.client.statemachine.agent;

import com.alibaba.cola.statemachine.StateMachine;
import com.xiaoniucode.etp.client.TunnelClient;
import com.xiaoniucode.etp.client.config.AppConfig;
import com.xiaoniucode.etp.client.health.HealthCheckManager;
import com.xiaoniucode.etp.client.identity.AgentIdentity;
import com.xiaoniucode.etp.client.transport.ControlFrameHandler;
import com.xiaoniucode.etp.client.transport.connection.DirectPool;
import com.xiaoniucode.etp.client.transport.connection.MultiplexPool;
import com.xiaoniucode.etp.core.enums.AgentType;
import com.xiaoniucode.etp.core.transport.AbstractAgentContext;
import com.xiaoniucode.etp.core.transport.TunnelEntry;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.EventLoopGroup;
import io.netty.handler.ssl.SslContext;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.util.concurrent.atomic.AtomicInteger;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class AgentContext extends AbstractAgentContext {
    private AgentState state = AgentState.IDLE;
    private AppConfig config;
    private SslContext tlsContext;
    private Bootstrap controlBootstrap;
    private Bootstrap serverBootstrap;
    private EventLoopGroup controlWorkerGroup;
    private EventLoopGroup serverWorkerGroup;
    private boolean authenticated;
    private TunnelClient tunnelClient;
    private DirectPool directPool;
    private MultiplexPool multiplexPool;
    private ControlFrameHandler controlFrameHandler;
    private HealthCheckManager healthCheckManager;
    private AgentIdentity agentIdentity;
    private final AtomicInteger retryCount = new AtomicInteger(0);
    private StateMachine<AgentState, AgentEvent, AgentContext> stateMachine;

    public AgentContext(AppConfig config, StateMachine<AgentState, AgentEvent, AgentContext> stateMachine) {
        this.config = config;
        this.stateMachine=stateMachine;
    }

    public AgentType getAgentType() {
        return config.getAgentType();
    }

    public void fireEvent(AgentEvent clientEvent) {
        stateMachine.fireEvent(state, clientEvent, this);
    }

    public TunnelEntry getConn(boolean encrypt, boolean multiplex) {
        TunnelEntry tunnelEntry;
        if (multiplex) {
            tunnelEntry = multiplexPool.acquire(encrypt);
        } else {
            tunnelEntry = directPool.borrow(encrypt);
        }
        return tunnelEntry;
    }
}
