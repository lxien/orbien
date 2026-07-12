package io.github.lxien.orbien.server.statemachine.agent.action;

import io.github.lxien.orbien.core.message.TMSP;
import io.github.lxien.orbien.core.message.TMSPFrame;
import io.github.lxien.orbien.core.utils.ChannelUtils;
import io.github.lxien.orbien.server.event.AgentOfflineEvent;
import io.github.lxien.orbien.server.manager.ProxyManager;
import io.github.lxien.orbien.server.notify.EventBus;
import io.github.lxien.orbien.server.statemachine.agent.*;
import io.github.lxien.orbien.server.statemachine.stream.StreamManager;
import io.github.lxien.orbien.server.transport.connection.DirectConnectionPool;
import io.github.lxien.orbien.server.transport.connection.MultiplexConnectionPool;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;


@Component
public class GoawayAction extends AgentBaseAction {
    private final InternalLogger logger = InternalLoggerFactory.getInstance(GoawayAction.class);
    @Autowired
    private AgentManager agentManager;
    @Autowired
    private StreamManager streamManager;
    @Autowired
    private DirectConnectionPool directConnectionPool;
    @Autowired
    private MultiplexConnectionPool multiplexConnectionPool;
    @Autowired
    private ProxyManager proxyManager;
    @Autowired
    private EventBus eventBus;

    @Override
    protected void doExecute(AgentState from, AgentState to, AgentEvent event, AgentContext context) {
        AgentInfo agentInfo = context.getAgentInfo();
        String agentId = agentInfo != null ? agentInfo.getAgentId() : null;
        Channel control = context.getControl();

        if (!StringUtils.hasText(agentId)) {
            logger.warn("Goaway 时未找到客户端信息, connectionId={}", context.getConnectionId());
            agentManager.detachControlChannel(context);
            agentManager.removeOrphanConnection(context.getConnectionId());
            if (control != null) {
                ChannelUtils.closeOnFlush(control);
            }
            return;
        }

        if (agentManager.getAgentContext(agentId).isEmpty()) {
            logger.debug("客户端 {} 资源已清理，跳过重复 Goaway", agentId);
            if (control != null && control.isActive()) {
                ChannelUtils.closeOnFlush(control);
            }
            return;
        }

        if (event == AgentEvent.LOCAL_GOAWAY && control != null && control.isActive()) {
            logger.info("客户端 {} 强制下线，发送 GOAWAY", agentId);
            control.eventLoop().execute(() ->
                    control.writeAndFlush(new TMSPFrame(0, TMSP.MSG_GOAWAY))
                            .addListener((ChannelFutureListener) future -> {
                                if (!future.isSuccess()) {
                                    logger.debug("客户端 {} GOAWAY 发送失败", agentId);
                                }
                                cleanupResources(context, agentInfo, agentId, control);
                            })
            );
            return;
        }
        cleanupResources(context, agentInfo, agentId, control);
    }

    private void cleanupResources(AgentContext context, AgentInfo agentInfo, String agentId, Channel control) {
        logger.debug("客户端 {} 断开，开始清理资源", agentId);
        try {
            streamManager.fireCloseByAgent(agentId);
            directConnectionPool.offline(agentId);
            multiplexConnectionPool.offline(agentId);
            agentManager.removeAgentContext(agentId);
            proxyManager.onAgentOffline(agentId);
            if (control != null) {
                ChannelUtils.closeOnFlush(control);
            }
            publishOfflineEvent(agentInfo);
            logger.info("客户端 {} 资源清理完成", agentId);
        } catch (Exception e) {
            logger.error("客户端 {} 资源清理失败", agentId, e);
        }
    }

    private void publishOfflineEvent(AgentInfo agentInfo) {
        if (agentInfo == null || !StringUtils.hasText(agentInfo.getAgentId())) {
            return;
        }
        eventBus.publishSync(new AgentOfflineEvent(agentInfo.getAgentId()));
    }
}
