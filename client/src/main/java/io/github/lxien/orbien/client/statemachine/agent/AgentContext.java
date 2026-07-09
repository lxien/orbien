package io.github.lxien.orbien.client.statemachine.agent;

import com.alibaba.cola.statemachine.StateMachine;
import io.github.lxien.orbien.client.TunnelClient;
import io.github.lxien.orbien.client.config.AppConfig;
import io.github.lxien.orbien.client.identity.AgentIdentity;
import io.github.lxien.orbien.client.filetransfer.FileTransferControlHandler;
import io.github.lxien.orbien.client.transport.ControlFrameHandler;
import io.github.lxien.orbien.client.transport.connection.TransportPoolManager;
import io.github.lxien.orbien.core.enums.AgentType;
import io.github.lxien.orbien.core.transport.AbstractAgentContext;
import io.github.lxien.orbien.core.transport.TunnelEntry;
import io.github.lxien.orbien.core.enums.TransportProtocol;
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
    private Bootstrap serverBootstrap;
    private Bootstrap udpServerBootstrap;
    private EventLoopGroup controlWorkerGroup;
    private EventLoopGroup serverWorkerGroup;
    private boolean authenticated;
    private TunnelClient tunnelClient;
    private TransportPoolManager poolManager = new TransportPoolManager();
    private ControlFrameHandler controlFrameHandler;
    private FileTransferControlHandler fileTransferControlHandler = new FileTransferControlHandler();
    private AgentIdentity agentIdentity;
    private final AtomicInteger retryCount = new AtomicInteger(0);
    private StateMachine<AgentState, AgentEvent, AgentContext> stateMachine;
    /**
     * 收到停止指令后拒绝重连
     */
    private volatile boolean shuttingDown;

    public AgentContext(AppConfig config, StateMachine<AgentState, AgentEvent, AgentContext> stateMachine) {
        this.config = config;
        this.stateMachine = stateMachine;
    }

    public AgentType getAgentType() {
        return config.getAgentType();
    }

    public void fireEvent(AgentEvent clientEvent) {
        if (shuttingDown && clientEvent != AgentEvent.REMOTE_GOAWAY && clientEvent != AgentEvent.LOCAL_GOAWAY) {
            return;
        }
        stateMachine.fireEvent(state, clientEvent, this);
    }

    public boolean isShuttingDown() {
        return shuttingDown || state == AgentState.SHUTDOWN;
    }

    public void markShuttingDown() {
        shuttingDown = true;
    }

    public TunnelEntry getConn(TransportProtocol protocol, boolean encrypt, boolean multiplex) {
        return poolManager.acquire(protocol, encrypt, multiplex);
    }
}
