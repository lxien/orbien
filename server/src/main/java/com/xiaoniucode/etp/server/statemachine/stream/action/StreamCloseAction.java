package com.xiaoniucode.etp.server.statemachine.stream.action;

import com.xiaoniucode.etp.core.message.TMSP;
import com.xiaoniucode.etp.core.message.TMSPFrame;
import com.xiaoniucode.etp.core.transport.AttributeKeys;
import com.xiaoniucode.etp.core.transport.NettyConstants;
import com.xiaoniucode.etp.core.transport.TunnelEntry;
import com.xiaoniucode.etp.core.transport.UdpSessionKey;
import com.xiaoniucode.etp.core.utils.ChannelUtils;
import com.xiaoniucode.etp.server.metrics.MetricsCollector;
import com.xiaoniucode.etp.server.statemachine.agent.AgentInfo;
import com.xiaoniucode.etp.server.loadbalance.LeastConnHooks;
import com.xiaoniucode.etp.server.statemachine.agent.AgentContext;
import com.xiaoniucode.etp.server.statemachine.stream.StreamEvent;
import com.xiaoniucode.etp.server.statemachine.stream.StreamManager;
import com.xiaoniucode.etp.server.statemachine.stream.StreamState;
import com.xiaoniucode.etp.server.statemachine.stream.StreamContext;
import com.xiaoniucode.etp.server.transport.connection.DirectConnectionPool;
import com.xiaoniucode.etp.server.transport.connection.MultiplexConnectionPool;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelPipeline;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

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

    @Override
    protected void doExecute(StreamState from, StreamState to, StreamEvent event, StreamContext context) {
        logger.debug("开启清理流 {} 相关资源", context.getStreamId());
        int streamId = context.getStreamId();
        Channel visitor = context.getVisitor();

        leastConnHooks.onStreamClosed(context);
        metricsCollector.onChannelInactive(context.getProxyId());

        ByteBuf byteBuf = visitor.attr(AttributeKeys.PENDING_READ).get();
        if (byteBuf != null && byteBuf.refCnt() > 0) {
            byteBuf.release();
        }
        if (!context.isDatagram()) {
            ChannelUtils.closeOnFlush(visitor);
        } else if (context.getVisitorAddress() != null && context.getListenerPort() != null) {
            streamManager.unregisterUdpSession(
                   UdpSessionKey.of(context.getListenerPort(), context.getVisitorAddress()));
        }
        AgentContext agentContext = context.getAgentContext();
        TunnelEntry tunnelEntry = context.getTunnelEntry();
        if (tunnelEntry != null) {
            if (context.isDirectConnection()) {
                AgentInfo agentInfo = agentContext.getAgentInfo();
                logger.debug("回收客户端 {} 独立连接 {}", agentInfo.getAgentId(), tunnelEntry.getTunnelId());
                directConnectionPool.release(agentInfo.getAgentId(), tunnelEntry);
                //清除直接隧道消息处理器
                Channel tunnel = tunnelEntry.getChannel();
                ChannelPipeline tunnelPipeline = tunnel.pipeline();
                ChannelHandler channelHandler = tunnelPipeline.get(NettyConstants.DIRECT_TUNNEL_BRIDGE_HANDLER);
                if (channelHandler != null) {
                    tunnelPipeline.remove(NettyConstants.DIRECT_TUNNEL_BRIDGE_HANDLER);
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
}
