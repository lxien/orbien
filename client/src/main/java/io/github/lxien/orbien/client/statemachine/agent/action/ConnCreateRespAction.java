package io.github.lxien.orbien.client.statemachine.agent.action;

import io.github.lxien.orbien.client.statemachine.ContextConstants;
import io.github.lxien.orbien.client.statemachine.agent.AgentContext;
import io.github.lxien.orbien.client.statemachine.agent.AgentEvent;
import io.github.lxien.orbien.client.statemachine.agent.AgentState;
import io.github.lxien.orbien.core.message.Message;
import io.github.lxien.orbien.core.transport.TunnelEntry;
import io.github.lxien.orbien.core.enums.TransportProtocol;
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
        TransportProtocol protocol = resolveProtocol(agentContext, resp.getTunnelId());

        Message.Status status = resp.getStatus();
        if (status.getCode() == 1) {
            logger.warn("隧道创建失败: {}", status.getMessage());
            removeCreateFailConn(agentContext, protocol, multiplex, encrypt, resp.getTunnelId());
            return;
        }
        TunnelEntry tunnelEntry;
        if (multiplex) {
            tunnelEntry = agentContext.getPoolManager().activateMultiplex(protocol, encrypt);
            logger.debug("[传输] 多路复用隧道激活 protocol={} encrypt={} tunnelId={} channelClass={}",
                    protocol.getName(), encrypt,
                    tunnelEntry != null ? tunnelEntry.getTunnelId() : null,
                    tunnelEntry != null && tunnelEntry.getChannel() != null
                            ? tunnelEntry.getChannel().getClass().getSimpleName() : null);
        } else {
            tunnelEntry = agentContext.getPoolManager().activateDirect(protocol, resp.getTunnelId());
            logger.debug("[传输] 独立隧道激活 protocol={} tunnelId={}", protocol.getName(), resp.getTunnelId());
        }
    }

    private TransportProtocol resolveProtocol(AgentContext agentContext, String tunnelId) {
        TransportProtocol protocol = agentContext.getAndRemoveAs(ContextConstants.TRANSPORT_PROTOCOL, TransportProtocol.class);
        if (protocol != null) {
            return protocol;
        }
        protocol = agentContext.getPoolManager().findProtocolByTunnelId(tunnelId);
        if (protocol != null) {
            logger.debug("[传输] 从待激活隧道推断协议={} tunnelId={}", protocol.getName(), tunnelId);
            return protocol;
        }
        return agentContext.getConfig().getTransportConfig().getProtocol();
    }

    private void removeCreateFailConn(AgentContext agentContext, TransportProtocol protocol,
                                      boolean multiplex, boolean encrypt, String tunnelId) {
        if (multiplex) {
            agentContext.getPoolManager().clearMultiplex(protocol, encrypt);
            return;
        }
        agentContext.getPoolManager().removeDirect(tunnelId);
    }
}
