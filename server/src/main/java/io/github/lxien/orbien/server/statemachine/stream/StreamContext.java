package io.github.lxien.orbien.server.statemachine.stream;

import com.alibaba.cola.statemachine.StateMachine;
import io.github.lxien.orbien.core.domain.ProxyConfig;
import io.github.lxien.orbien.core.domain.Target;
import io.github.lxien.orbien.core.enums.ProtocolType;
import io.github.lxien.orbien.core.message.TMSPFrame;
import io.github.lxien.orbien.core.transport.AbstractStreamContext;
import io.github.lxien.orbien.core.transport.AttributeKeys;
import io.github.lxien.orbien.server.statemachine.agent.AgentContext;
import io.github.lxien.orbien.server.transport.BandwidthLimiter;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.net.InetSocketAddress;
import java.util.ArrayDeque;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class StreamContext extends AbstractStreamContext {
    private StreamState state = StreamState.IDLE;
    private Channel visitor;
    private ProxyConfig proxyConfig;
    private String sourceAddress;
    private Target target;
    private ProtocolType protocol = ProtocolType.TCP;
    private BandwidthLimiter bandwidthLimiter;
    private AgentContext agentContext;
    private StreamManager streamManager;
    private StateMachine<StreamState, StreamEvent, StreamContext> stateMachine;

    private final ArrayDeque<TMSPFrame> messagesQueue = new ArrayDeque<>();
    private InetSocketAddress visitorAddress;
    private ByteBuf pendingFirstPacket;

    public StreamContext(int streamId, StateMachine<StreamState, StreamEvent, StreamContext> streamStateMachine) {
        this.streamId = streamId;
        this.stateMachine = streamStateMachine;
    }

    /**
     * Agent可能为空
     */
    public boolean hasAgent() {
        return agentContext != null;
    }

    public String getProxyId() {
        if (proxyConfig == null) {
            return null;
        }
        return proxyConfig.getProxyId();
    }

    public String getVisitorDomain() {
        if (visitor == null) {
            return null;
        }
        return visitor.attr(AttributeKeys.VISIT_DOMAIN).get();
    }

    public Integer getListenerPort() {
        if (visitor == null) {
            return null;
        }
        InetSocketAddress sa = (InetSocketAddress) visitor.localAddress();
        return sa.getPort();
    }

    public String getAgentId() {
        return agentContext.getAgentId();
    }

    public Channel getControl() {
        return agentContext.getControl();
    }

    public void fireEvent(StreamEvent event) {
        stateMachine.fireEvent(state, event, this);
    }
}