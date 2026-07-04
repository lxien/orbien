package io.github.lxien.orbien.server.statemachine.agent.action;

import io.github.lxien.orbien.core.message.TMSP;
import io.github.lxien.orbien.core.message.TMSPFrame;
import io.github.lxien.orbien.core.utils.ChannelUtils;
import io.github.lxien.orbien.server.manager.ProxyManager;
import io.github.lxien.orbien.server.statemachine.agent.*;
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

    @Override
    protected void doExecute(AgentState from, AgentState to, AgentEvent event, AgentContext context) {
        logger.debug("开始清理客户端资源，事件：{}", event);
        AgentInfo agentInfo = context.getAgentInfo();
        if (agentInfo == null) {
            logger.warn("客户端断开，未找到客户端信息，连接ID：{}", context.getConnectionId());
            return;
        }
        String agentId = agentInfo.getAgentId();
        logger.debug("{} 客户端断开，开始清理资源", agentId);
        try {
            // 清理流资源
            streamManager.fireCloseByAgent(context.getAgentId());
            // 清理隧道资源
            directConnectionPool.offline(agentId);
            multiplexConnectionPool.offline(agentId);
            //清理连接上下文
            agentManager.removeAgentContext(agentId);
            proxyManager.onAgentOffline(agentId);
            if (event == AgentEvent.LOCAL_GOAWAY) {
                Channel control = context.getControl();
                control.writeAndFlush(new TMSPFrame(0, TMSP.MSG_GOAWAY)).addListener((ChannelFutureListener) future -> {
                    if (!future.isSuccess()) {
                        logger.debug("{} GOAWAY 发送失败（可能连接已断）", agentId);
                    }
                    ChannelUtils.closeOnFlush(control);
                });
            }
            logger.info("{} 客户端资源清理完成", agentId);
        } catch (Exception e) {
            logger.error("{} 资源清理过程中发生异常", agentId, e);
        }
    }
}
