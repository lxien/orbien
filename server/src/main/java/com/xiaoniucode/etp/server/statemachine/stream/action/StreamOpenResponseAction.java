package com.xiaoniucode.etp.server.statemachine.stream.action;

import com.xiaoniucode.etp.core.transport.AttributeKeys;
import com.xiaoniucode.etp.core.transport.TunnelEntry;
import com.xiaoniucode.etp.server.metrics.MetricsCollector;
import com.xiaoniucode.etp.server.statemachine.agent.AgentInfo;
import com.xiaoniucode.etp.server.loadbalance.LeastConnHooks;
import com.xiaoniucode.etp.server.statemachine.agent.AgentContext;
import com.xiaoniucode.etp.server.statemachine.stream.*;
import com.xiaoniucode.etp.server.transport.connection.DirectConnectionPool;
import com.xiaoniucode.etp.server.transport.connection.MultiplexConnectionPool;
import com.xiaoniucode.etp.core.transport.TunnelBridge;
import com.xiaoniucode.etp.server.transport.bridge.TunnelBridgeFactory;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 流打开成功处理
 */
@Component
public class StreamOpenResponseAction extends StreamBaseAction {
    private final InternalLogger logger = InternalLoggerFactory.getInstance(StreamOpenResponseAction.class);
    @Autowired
    private DirectConnectionPool directConnectionPool;
    @Autowired
    private MultiplexConnectionPool multiplexConnectionPool;
    @Autowired
    private LeastConnHooks leastConnHooks;
    @Autowired
    private MetricsCollector metricsCollector;
    @Override
    protected void doExecute(StreamState from, StreamState to, StreamEvent event, StreamContext context) {
        logger.debug("收到流 {} 打开响应", context.getStreamId());
        String tunnelId = context.getAndRemoveAs(StreamConstants.TUNNEL_ID, String.class);
        AgentContext agentContext = context.getAgentContext();
        AgentInfo agentInfo = agentContext.getAgentInfo();
        String agentId = agentInfo.getAgentId();
        TunnelEntry tunnelEntry;
        if (context.isMultiplex()) {
            tunnelEntry = multiplexConnectionPool.acquire(agentId, context.isEncrypt());
        } else {
            tunnelEntry = directConnectionPool.borrow(agentId, tunnelId, context.isEncrypt());
        }
        if (tunnelEntry == null) {
            logger.warn("连接池没有可用连接，关闭流");
            context.fireEvent(StreamEvent.STREAM_LOCAL_CLOSE);
            return;
        }
        if (!tunnelEntry.isActive()){
            logger.warn("连接不可用 {} 关闭流",tunnelEntry.getTunnelId());
            context.fireEvent(StreamEvent.STREAM_LOCAL_CLOSE);
            return;
        }
        context.setTunnelEntry(tunnelEntry);
        Channel visitor = context.getVisitor();
        TunnelBridge tunnelBridge;
        if (context.isDatagram()) {
            tunnelBridge = TunnelBridgeFactory.buildUdpMux(context);
            logger.debug("UDP 共享隧道建立成功，访问目标: {}", context.getTarget());
        } else if (context.isMultiplex()) {
            tunnelBridge = TunnelBridgeFactory.buildMux(context);
            logger.debug("共享隧道建立成功，访问目标: {}", context.getTarget());
        } else {
            tunnelBridge = TunnelBridgeFactory.buildDirect(context);
            logger.debug("独立隧道建立成功，隧道ID: {}", tunnelEntry.getTunnelId());
        }
        tunnelBridge.open();
        context.setTunnelBridge(tunnelBridge);

        StreamManager streamManager = context.getStreamManager();
        //初始化索引映射关系
        streamManager.initStreamIndexes(context);
        //负载均衡 最少连接数
        leastConnHooks.onStreamOpened(context);
        //增加连接数量，用于监控统计
        metricsCollector.onChannelActive(context.getProxyId(),agentInfo.getAgentType());
        //如果是 HTTP协议需要发送首次建立建立的时候读取到的第一个包
        if (context.getProtocol().isHttp()) {
            relayHttpFirstPackage(context,visitor, tunnelBridge);
        }
        if (context.isDatagram()) {
            relayUdpFirstPacket(context, tunnelBridge);
        }
        // TCP 连接按流控制读；UDP 复用 DatagramChannel，必须始终保持可读
        visitor.config().setOption(ChannelOption.AUTO_READ, true);
        logger.debug("流 {} 打开成功，可以从访问者读数据", context.getStreamId());
    }

    /**
     * 发送HTTP 协议首次缓存的第一个数据包
     */
    public void relayHttpFirstPackage(StreamContext context,Channel visitor, TunnelBridge tunnelBridge) {
        logger.debug("转发HTTP第一个数据包");
        ByteBuf cached = visitor.attr(AttributeKeys.HTTP_FIRST_PACKET).get();
        tunnelBridge.forwardToLocal(cached);
        visitor.attr(AttributeKeys.HTTP_FIRST_PACKET).set(null);
    }

    public void relayUdpFirstPacket(StreamContext context, TunnelBridge tunnelBridge) {
        ByteBuf cached = context.getPendingFirstPacket();
        if (cached != null && cached.refCnt() > 0) {
            logger.debug("转发 UDP 第一个数据包 streamId={}", context.getStreamId());
            try {
                tunnelBridge.forwardToLocal(cached);
            } finally {
                context.setPendingFirstPacket(null);
                ReferenceCountUtil.release(cached);
            }
        }
    }
}
