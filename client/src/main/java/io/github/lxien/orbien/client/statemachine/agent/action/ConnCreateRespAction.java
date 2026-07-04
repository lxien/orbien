package io.github.lxien.orbien.client.statemachine.agent.action;

import io.github.lxien.orbien.client.statemachine.agent.AgentContext;
import io.github.lxien.orbien.client.statemachine.ContextConstants;
import io.github.lxien.orbien.client.statemachine.agent.AgentEvent;
import io.github.lxien.orbien.client.statemachine.agent.AgentState;
import io.github.lxien.orbien.client.transport.connection.DirectPool;
import io.github.lxien.orbien.client.transport.connection.MultiplexPool;
import io.github.lxien.orbien.core.message.Message;
import io.github.lxien.orbien.core.transport.TunnelEntry;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

public class ConnCreateRespAction extends AgentBaseAction {
    private final InternalLogger logger = InternalLoggerFactory.getInstance(ConnCreateRespAction.class);

    @Override
    protected void doExecute(AgentState from, AgentState to, AgentEvent event, AgentContext agentContext) {
        logger.debug("创建隧道成功");
        boolean encrypt = agentContext.getAndRemoveAs(ContextConstants.ENCRYPT, Boolean.class);
        boolean multiplex = agentContext.getAndRemoveAs(ContextConstants.MULTIPLEX, Boolean.class);

        Message.CreateConnectionResponse resp = agentContext.getAndRemoveAs(ContextConstants.CREATE_CONN_RESP,
                Message.CreateConnectionResponse.class);
        Message.Status status = resp.getStatus();
        if (status.getCode() == 1) {
            logger.warn("隧道创建失败: {}", status.getMessage());
            removeCreateFailConn(agentContext, multiplex, resp.getTunnelId());
            return;
        }
        TunnelEntry tunnelEntry;
        if (multiplex) {
            MultiplexPool multiplexPool = agentContext.getMultiplexPool();
            tunnelEntry = multiplexPool.activeTunnel(encrypt);
            logger.debug("激活共享隧道: tunnelId={} 激活状态：{} 是否加密：{}", tunnelEntry.getTunnelId(), tunnelEntry.isActive(),encrypt);
        } else {
            DirectPool directPool = agentContext.getDirectPool();
            tunnelEntry = directPool.activateTunnel(resp.getTunnelId());
            logger.debug("激活独立隧道: tunnelId={} 激活状态：{} 是否加密：{}", tunnelEntry.getTunnelId(), tunnelEntry.isActive(),encrypt);
        }
    }

    private void removeCreateFailConn(AgentContext agentContext, boolean multiplex, String tunnelId) {
        if (multiplex) {
            MultiplexPool multiplexPool = agentContext.getMultiplexPool();
            multiplexPool.clearTunnel(true);
            return;
        }
        DirectPool directPool = agentContext.getDirectPool();
        directPool.removeTunnel(tunnelId);
    }
}
