package io.github.lxien.orbien.server.statemachine.stream;

import com.alibaba.cola.statemachine.StateMachine;
import io.github.lxien.orbien.core.domain.ProxyConfig;
import io.github.lxien.orbien.core.domain.Target;
import io.github.lxien.orbien.core.enums.ProtocolType;
import io.github.lxien.orbien.core.message.TMSPFrame;
import io.github.lxien.orbien.core.transport.AbstractStreamContext;
import io.github.lxien.orbien.core.transport.AttributeKeys;
import io.github.lxien.orbien.server.statemachine.agent.AgentContext;
import io.github.lxien.orbien.server.inspector.HttpStreamCapture;
import io.github.lxien.orbien.server.transport.BandwidthLimiter;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.util.ReferenceCountUtil;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.net.InetSocketAddress;
import java.util.ArrayDeque;
import java.util.concurrent.atomic.AtomicBoolean;

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

    /**
     * 隧道/写入失败后立即置位，阻止访客继续 pump 数据
     */
    private final AtomicBoolean localForwardingAborted = new AtomicBoolean(false);

    /**
     * 独立隧道：服务端已透传，等待客户端确认后再开启 visitor 读取
     */
    private final AtomicBoolean awaitingClientPassthroughAck = new AtomicBoolean(false);

    private HttpStreamCapture httpStreamCapture;

    public boolean isAwaitingClientPassthroughAck() {
        return awaitingClientPassthroughAck.get();
    }

    public boolean compareAndClearAwaitingClientPassthroughAck() {
        return awaitingClientPassthroughAck.compareAndSet(true, false);
    }

    public void markAwaitingClientPassthroughAck() {
        awaitingClientPassthroughAck.set(true);
    }

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

    public boolean isLocalForwardingAborted() {
        return localForwardingAborted.get();
    }

    /**
     * 立即停止从访客读取并丢弃尚未转发的缓存，避免向已断开的隧道反复写入。
     */
    public void abortLocalForwarding() {
        if (!localForwardingAborted.compareAndSet(false, true)) {
            return;
        }
        Channel v = visitor;
        if (v != null && v.isActive()) {
            v.config().setOption(ChannelOption.AUTO_READ, false);
        }
        ByteBuf pending;
        while ((pending = pollPending()) != null) {
            ReferenceCountUtil.release(pending);
        }
        if (v != null) {
            ByteBuf httpFirst = v.attr(AttributeKeys.HTTP_FIRST_PACKET).getAndSet(null);
            if (httpFirst != null) {
                ReferenceCountUtil.release(httpFirst);
            }
        }
    }

    public boolean canAcceptVisitorData() {
        if (localForwardingAborted.get()) {
            return false;
        }
        return state == StreamState.OPENED || state == StreamState.OPENING || state == StreamState.PAUSED;
    }
}
