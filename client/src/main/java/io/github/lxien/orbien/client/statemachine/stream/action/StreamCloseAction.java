package io.github.lxien.orbien.client.statemachine.stream.action;

import io.github.lxien.orbien.client.statemachine.agent.AgentContext;
import io.github.lxien.orbien.client.statemachine.stream.StreamContext;
import io.github.lxien.orbien.client.statemachine.stream.StreamEvent;
import io.github.lxien.orbien.client.statemachine.stream.StreamManager;
import io.github.lxien.orbien.client.statemachine.stream.StreamState;
import io.github.lxien.orbien.client.transport.ControlFrameHandler;
import io.github.lxien.orbien.client.transport.connection.TransportPoolManager;
import io.github.lxien.orbien.core.message.TMSP;
import io.github.lxien.orbien.core.message.TMSPFrame;
import io.github.lxien.orbien.core.transport.IdleCheckHandler;
import io.github.lxien.orbien.core.transport.NettyConstants;
import io.github.lxien.orbien.core.transport.TunnelEntry;
import io.github.lxien.orbien.core.transport.direct.DirectTunnelLifecycle;
import io.github.lxien.orbien.core.utils.ChannelUtils;
import io.netty.channel.Channel;
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
            TransportPoolManager poolManager = agentContext.getPoolManager();
            Channel tunnel = tunnelEntry.getChannel();
            ControlFrameHandler controlFrameHandler = agentContext.getControlFrameHandler();
            Runnable release = () -> poolManager.releaseDirect(tunnelEntry);
            if (DirectTunnelLifecycle.isPassthroughActive(tunnel)) {
                if (!DirectTunnelLifecycle.canRestoreControlStack(tunnel)) {
                    logger.debug("独立隧道已关闭或 pipeline 不可用，跳过重栈恢复 tunnelId={}", tunnelEntry.getTunnelId());
                    poolManager.removeDirect(tunnelEntry.getTunnelId());
                } else {
                    DirectTunnelLifecycle.restoreControlStack(tunnel, pipeline -> {
                        if (pipeline.get(NettyConstants.IDLE_CHECK_HANDLER) == null) {
                            pipeline.addLast(NettyConstants.IDLE_CHECK_HANDLER, new IdleCheckHandler());
                        }
                        if (pipeline.get(NettyConstants.CONTROL_FRAME_HANDLER) == null && controlFrameHandler != null) {
                            pipeline.addLast(NettyConstants.CONTROL_FRAME_HANDLER, controlFrameHandler);
                        }
                    }).addListener(f -> {
                        if (!f.isSuccess()) {
                            logger.warn("恢复独立隧道控制栈失败 tunnelId={}，关闭隧道", tunnelEntry.getTunnelId(), f.cause());
                            ChannelUtils.closeOnFlush(tunnel);
                            poolManager.removeDirect(tunnelEntry.getTunnelId());
                        } else if (tunnel.isActive()) {
                            release.run();
                        } else {
                            poolManager.removeDirect(tunnelEntry.getTunnelId());
                        }
                    });
                }
            } else {
                release.run();
            }
        }
        ChannelUtils.closeOnFlush(server);

        context.discardPending();
        StreamManager.removeStreamContext(streamId);
        if (event == StreamEvent.STREAM_LOCAL_CLOSE) {
            logger.debug("通知对端关闭流 {}", context.getStreamId());
            context.getControl().writeAndFlush(new TMSPFrame(streamId, TMSP.MSG_STREAM_CLOSE));
        }
        logger.debug("关闭流 {} 成功 - localIp={} localPort={}", context.getStreamId(), context.getLocalIp(), context.getLocalPort());
    }
}
