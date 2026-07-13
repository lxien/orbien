package io.github.lxien.orbien.server.statemachine.stream;

import com.alibaba.cola.statemachine.StateMachine;
import io.github.lxien.orbien.core.domain.ProxyConfig;
import io.github.lxien.orbien.core.domain.Target;
import io.github.lxien.orbien.core.enums.ProtocolType;
import io.github.lxien.orbien.core.transport.AbstractStreamContext;
import io.github.lxien.orbien.core.transport.AttributeKeys;
import io.github.lxien.orbien.core.utils.ChannelUtils;
import io.github.lxien.orbien.server.statemachine.agent.AgentContext;
import io.github.lxien.orbien.server.inspector.HttpStreamCapture;
import io.github.lxien.orbien.server.transport.traffic.BandwidthLimiter;
import io.github.lxien.orbien.server.transport.traffic.RemoteProducerGate;
import io.github.lxien.orbien.server.transport.traffic.StreamTrafficShaper;
import io.github.lxien.orbien.server.transport.traffic.VisitorReadGate;
import io.github.lxien.orbien.server.utils.NettyHttpUtils;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.util.ReferenceCountUtil;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicBoolean;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class StreamContext extends AbstractStreamContext {

    public static final int VISITOR_PAUSE_RATE_LIMIT = VisitorReadGate.PAUSE_RATE_LIMIT;
    public static final int VISITOR_PAUSE_BACKPRESSURE = VisitorReadGate.PAUSE_BACKPRESSURE;
    public static final int VISITOR_PAUSE_REMOTE = VisitorReadGate.PAUSE_REMOTE;
    public static final int VISITOR_PAUSE_OPENING = VisitorReadGate.PAUSE_OPENING;

    public static final int REMOTE_PAUSE_RATE_LIMIT = RemoteProducerGate.REASON_RATE_LIMIT;
    public static final int REMOTE_PAUSE_VISITOR_BACKPRESSURE = RemoteProducerGate.REASON_VISITOR_BACKPRESSURE;

    private StreamState state = StreamState.IDLE;
    private Channel visitor;
    private ProxyConfig proxyConfig;
    private String sourceAddress;
    private Target target;
    private ProtocolType protocol = ProtocolType.TCP;
    private BandwidthLimiter bandwidthLimiter;
    private StreamTrafficShaper trafficShaper;
    private final VisitorReadGate visitorReadGate = new VisitorReadGate();
    private final RemoteProducerGate remoteProducerGate =
            new RemoteProducerGate(this::getStreamId, this::getControl);
    private AgentContext agentContext;
    private StreamManager streamManager;
    private StateMachine<StreamState, StreamEvent, StreamContext> stateMachine;

    private InetSocketAddress visitorAddress;
    private ByteBuf pendingFirstPacket;

    private final AtomicBoolean localForwardingAborted = new AtomicBoolean(false);
    private final AtomicBoolean awaitingClientPassthroughAck = new AtomicBoolean(false);

    private HttpStreamCapture httpStreamCapture;
    private int gatewayErrorStatus = 502;

    public StreamContext(int streamId, StateMachine<StreamState, StreamEvent, StreamContext> streamStateMachine) {
        this.streamId = streamId;
        this.stateMachine = streamStateMachine;
    }

    public void setBandwidthLimiter(BandwidthLimiter bandwidthLimiter) {
        this.bandwidthLimiter = bandwidthLimiter;
        if (bandwidthLimiter != null && bandwidthLimiter.isEnabled()) {
            this.trafficShaper = new StreamTrafficShaper(bandwidthLimiter, this, visitorReadGate);
        } else {
            this.trafficShaper = null;
        }
    }

    public boolean isAwaitingClientPassthroughAck() {
        return awaitingClientPassthroughAck.get();
    }

    public boolean compareAndClearAwaitingClientPassthroughAck() {
        return awaitingClientPassthroughAck.compareAndSet(true, false);
    }

    public void markAwaitingClientPassthroughAck() {
        awaitingClientPassthroughAck.set(true);
    }

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

    public boolean canResumeVisitorRead() {
        return !isAwaitingClientPassthroughAck() && !isLocalForwardingAborted();
    }

    public void abortLocalForwarding() {
        if (!localForwardingAborted.compareAndSet(false, true)) {
            return;
        }
        if (trafficShaper != null) {
            trafficShaper.clear();
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
            if (httpFirst != null && httpFirst.refCnt() > 0) {
                ReferenceCountUtil.release(httpFirst);
            }
        }
        visitorReadGate.reset();
        remoteProducerGate.reset();
    }

    public boolean canAcceptVisitorData() {
        if (localForwardingAborted.get()) {
            return false;
        }
        return state == StreamState.OPENED || state == StreamState.OPENING || state == StreamState.PAUSED;
    }

    @Override
    public void forwardToLocal(ByteBuf payload, boolean sharedWithInbound) {
        if (trafficShaper != null && !trafficShaper.tryUpload(payload, sharedWithInbound)) {
            return;
        }
        forwardToLocalUnchecked(payload, sharedWithInbound);
    }

    @Override
    public void forwardToRemote(ByteBuf payload, boolean sharedWithInbound) {
        if (trafficShaper != null && !trafficShaper.tryDownload(payload, sharedWithInbound)) {
            return;
        }
        forwardToRemoteUnchecked(payload, sharedWithInbound);
    }

    public void forwardToLocalUnchecked(ByteBuf payload, boolean sharedWithInbound) {
        super.forwardToLocal(payload, sharedWithInbound);
    }

    public void forwardToRemoteUnchecked(ByteBuf payload, boolean sharedWithInbound) {
        super.forwardToRemote(payload, sharedWithInbound);
    }

    public void pauseVisitorRead(int reason) {
        if (datagram) {
            return;
        }
        visitorReadGate.pause(visitor, reason);
    }

    public void resumeVisitorRead(int reason) {
        if (datagram) {
            return;
        }
        visitorReadGate.resume(visitor, reason, canResumeVisitorRead());
    }

    public void pauseRemoteProducer(int reason) {
        if (datagram) {
            return;
        }
        remoteProducerGate.pause(reason);
    }

    public void resumeRemoteProducer(int reason) {
        if (datagram) {
            return;
        }
        remoteProducerGate.resume(reason);
    }

    public void onVisitorWritabilityChanged(boolean writable) {
        if (datagram) {
            return;
        }
        if (writable) {
            resumeRemoteProducer(REMOTE_PAUSE_VISITOR_BACKPRESSURE);
        } else {
            pauseRemoteProducer(REMOTE_PAUSE_VISITOR_BACKPRESSURE);
        }
    }

    public void rejectVisitorUpload() {
        Channel v = visitor;
        if (v == null || !v.isActive()) {
            return;
        }
        if (protocol.isHttp()) {
            NettyHttpUtils.sendHttpTooManyRequests(v)
                    .addListener(f -> ChannelUtils.closeOnFlush(v));
            return;
        }
        if (protocol.isTcp() || protocol.isSocks5()) {
            ChannelUtils.closeOnFlush(v);
        }
    }
}
