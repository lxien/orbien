package io.github.lxien.orbien.client.statemachine.stream.action;

import io.github.lxien.orbien.client.statemachine.agent.AgentContext;
import io.github.lxien.orbien.client.statemachine.stream.StreamContext;
import io.github.lxien.orbien.client.statemachine.stream.StreamEvent;
import io.github.lxien.orbien.client.statemachine.stream.StreamManager;
import io.github.lxien.orbien.client.statemachine.stream.StreamState;
import io.github.lxien.orbien.client.transport.connection.DirectPool;
import io.github.lxien.orbien.core.message.TMSP;
import io.github.lxien.orbien.core.message.TMSPFrame;
import io.github.lxien.orbien.core.transport.NettyConstants;
import io.github.lxien.orbien.core.transport.TunnelEntry;
import io.github.lxien.orbien.core.utils.ChannelUtils;
import io.netty.channel.Channel;
import io.netty.channel.ChannelPipeline;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

public class StreamCloseAction extends StreamBaseAction {
    private final InternalLogger logger = InternalLoggerFactory.getInstance(StreamCloseAction.class);

    @Override
    protected void doExecute(StreamState from, StreamState to, StreamEvent event, StreamContext context) {
        logger.debug("开始关闭流 {}", context.getStreamId());
        int streamId = context.getStreamId();

        Channel server = context.getServer();
        TunnelEntry tunnelEntry = context.getTunnelEntry();
        if (tunnelEntry != null && tunnelEntry.getTunnelType().isDirect()) {
            AgentContext agentContext = (AgentContext) context.getAgentContext();
            DirectPool directPool = agentContext.getDirectPool();
            directPool.release(tunnelEntry);

            Channel tunnel = tunnelEntry.getChannel();
            ChannelPipeline tunnelPipeline = tunnel.pipeline();
            if (tunnelPipeline.get(NettyConstants.DIRECT_TUNNEL_BRIDGE_HANDLER) != null) {
                tunnelPipeline.remove(NettyConstants.DIRECT_TUNNEL_BRIDGE_HANDLER);
            }
        }
        ChannelUtils.closeOnFlush(server);

        StreamManager.removeStreamContext(streamId);
        if (event == StreamEvent.STREAM_LOCAL_CLOSE) {
            logger.debug("通知对端关闭流 {}", context.getStreamId());
            context.getControl().writeAndFlush(new TMSPFrame(streamId, TMSP.MSG_STREAM_CLOSE));
        }
        logger.debug("关闭流 {} 成功 - localIp={} localPort={}", context.getStreamId(), context.getLocalIp(), context.getLocalPort());
    }
}
