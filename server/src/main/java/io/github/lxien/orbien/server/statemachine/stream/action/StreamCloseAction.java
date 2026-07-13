package io.github.lxien.orbien.server.statemachine.stream.action;

import io.github.lxien.orbien.core.message.TMSP;
import io.github.lxien.orbien.core.message.TMSPFrame;
import io.github.lxien.orbien.core.socks5.Socks5Constants;
import io.github.lxien.orbien.core.transport.AttributeKeys;
import io.github.lxien.orbien.core.transport.TunnelEntry;
import io.github.lxien.orbien.core.transport.UdpSessionKey;
import io.github.lxien.orbien.core.transport.direct.DirectTunnelLifecycle;
import io.github.lxien.orbien.core.utils.ChannelUtils;
import io.github.lxien.orbien.server.inspector.HttpCaptureRecord;
import io.github.lxien.orbien.server.inspector.HttpStreamCapture;
import io.github.lxien.orbien.server.inspector.InspectorBuffer;
import io.github.lxien.orbien.server.metrics.MetricsCollector;
import io.github.lxien.orbien.server.statemachine.agent.AgentInfo;
import io.github.lxien.orbien.server.loadbalance.LeastConnHooks;
import io.github.lxien.orbien.server.statemachine.agent.AgentContext;
import io.github.lxien.orbien.server.statemachine.stream.StreamConstants;
import io.github.lxien.orbien.server.statemachine.stream.StreamContext;
import io.github.lxien.orbien.server.statemachine.stream.StreamEvent;
import io.github.lxien.orbien.server.statemachine.stream.StreamManager;
import io.github.lxien.orbien.server.statemachine.stream.StreamState;
import io.github.lxien.orbien.server.utils.NettyHttpUtils;
import io.github.lxien.orbien.server.transport.socks5.Socks5ReplyHelper;
import io.github.lxien.orbien.server.transport.connection.DirectConnectionPool;
import io.github.lxien.orbien.server.transport.connection.MultiplexConnectionPool;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
public class StreamCloseAction extends StreamBaseAction {
    private final InternalLogger logger = InternalLoggerFactory.getInstance(StreamCloseAction.class);
    @Autowired
    private StreamManager streamManager;
    @Autowired
    private LeastConnHooks leastConnHooks;
    @Autowired
    private DirectConnectionPool directConnectionPool;
    @Autowired
    private MultiplexConnectionPool multiplexConnectionPool;
    @Autowired
    private MetricsCollector metricsCollector;
    @Autowired
    private InspectorBuffer inspectorBuffer;

    @Override
    protected void doExecute(StreamState from, StreamState to, StreamEvent event, StreamContext context) {
        finalizeHttpCapture(context);
        logger.debug("开启清理流 {} 相关资源", context.getStreamId());
        context.abortLocalForwarding();
        int streamId = context.getStreamId();
        Channel visitor = context.getVisitor();

        leastConnHooks.onStreamClosed(context);
        metricsCollector.onChannelInactive(context.getProxyId());

        ByteBuf byteBuf = visitor.attr(AttributeKeys.PENDING_READ).getAndSet(null);
        if (byteBuf != null && byteBuf.refCnt() > 0) {
            byteBuf.release();
        }
        ByteBuf httpFirst = visitor.attr(AttributeKeys.HTTP_FIRST_PACKET).getAndSet(null);
        if (httpFirst != null && httpFirst.refCnt() > 0) {
            ReferenceCountUtil.release(httpFirst);
        }
        ByteBuf pending;
        while ((pending = context.pollPending()) != null) {
            ReferenceCountUtil.release(pending);
        }
        if (!context.isDatagram()) {
            if (context.getProtocol().isHttpOrHttps()) {
                closeHttpVisitor(context, from, event, visitor);
            } else if (context.getProtocol().isSocks5()
                    && context.hasVariable(StreamConstants.SOCKS5_AWAIT_REPLY)) {
                context.removeVariable(StreamConstants.SOCKS5_AWAIT_REPLY);
                Socks5ReplyHelper.sendConnectFailure(visitor, Socks5Constants.REP_GENERAL_FAILURE);
            } else {
                ChannelUtils.closeOnFlush(visitor);
            }
        } else if (context.getVisitorAddress() != null && context.getListenerPort() != null) {
            streamManager.unregisterUdpSession(
                   UdpSessionKey.of(context.getListenerPort(), context.getVisitorAddress()));
        }
        AgentContext agentContext = context.getAgentContext();
        TunnelEntry tunnelEntry = context.getTunnelEntry();
        if (tunnelEntry != null) {
            if (context.isDirectConnection() && !context.isDatagram()) {
                AgentInfo agentInfo = agentContext.getAgentInfo();
                Channel tunnel = tunnelEntry.getChannel();
                logger.debug("独立隧道流关闭，丢弃连接 streamId={} tunnelId={} passthroughActive={}",
                        streamId, tunnelEntry.getTunnelId(), DirectTunnelLifecycle.isPassthroughActive(tunnel));
                DirectTunnelLifecycle.closeAfterPassthrough(tunnel);
                directConnectionPool.remove(agentInfo.getAgentId(), tunnelEntry.getTunnelId());
            } else if (context.isDirectConnection()) {
                AgentInfo agentInfo = agentContext.getAgentInfo();
                Channel tunnel = tunnelEntry.getChannel();
                if (tunnel.isActive()) {
                    logger.debug("回收客户端 {} 独立连接 {}", agentInfo.getAgentId(), tunnelEntry.getTunnelId());
                    directConnectionPool.release(agentInfo.getAgentId(), tunnelEntry);
                } else {
                    directConnectionPool.remove(agentInfo.getAgentId(), tunnelEntry.getTunnelId());
                }
            }
        }
        streamManager.removeStreamContext(streamId);
        //流可能是半打开状态，Agent可能为空
        if (event == StreamEvent.STREAM_LOCAL_CLOSE && context.hasAgent()) {
            logger.debug("通知对端关闭流");
            Channel control = agentContext.getControl();
            TMSPFrame frame = new TMSPFrame(streamId, TMSP.MSG_STREAM_CLOSE);
            control.writeAndFlush(frame);
        }

        logger.debug("关闭流: streamId={}", context.getStreamId());
    }

    private void closeHttpVisitor(StreamContext context, StreamState from, StreamEvent event, Channel visitor) {
        if (visitor == null || !visitor.isActive()) {
            return;
        }
        if (from == StreamState.OPENING || from == StreamState.FAILED) {
            int status = context.getGatewayErrorStatus();
            String message = gatewayMessage(status);
            NettyHttpUtils.sendHttpBinaryResponse(visitor, status, message,
                            "text/plain; charset=UTF-8", message.getBytes(StandardCharsets.UTF_8))
                    .addListener(f -> ChannelUtils.closeOnFlush(visitor));
            return;
        }
        ChannelUtils.closeOnFlush(visitor);
    }

    private static String gatewayMessage(int status) {
        return switch (status) {
            case 503 -> "Service Unavailable";
            case 504 -> "Gateway Timeout";
            default -> "Bad Gateway";
        };
    }

    private void finalizeHttpCapture(StreamContext context) {
        HttpStreamCapture capture = context.getHttpStreamCapture();
        if (capture == null) {
            return;
        }
        context.setHttpStreamCapture(null);
        HttpCaptureRecord record = capture.finalizeCapture();
        if (record != null) {
            inspectorBuffer.append(record);
        }
    }
}
