package io.github.lxien.orbien.client.statemachine.agent.action;

import io.github.lxien.orbien.client.statemachine.ContextConstants;
import io.github.lxien.orbien.client.statemachine.agent.AgentContext;
import io.github.lxien.orbien.client.statemachine.agent.AgentEvent;
import io.github.lxien.orbien.client.statemachine.agent.AgentState;
import io.github.lxien.orbien.core.enums.AgentType;
import io.github.lxien.orbien.core.message.Message;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

public class AuthRespAction extends AgentBaseAction {
    private final InternalLogger logger = InternalLoggerFactory.getInstance(AuthRespAction.class);

    @Override
    protected void doExecute(AgentState from, AgentState to, AgentEvent event, AgentContext context) {

        Message.AuthResponse authResponse = context.getAndRemoveAs(ContextConstants.AUTH_RESP,
                Message.AuthResponse.class);
        Message.Status status = authResponse.getStatus();
        int code = status.getCode();
        if (code == 0) {
            logger.info("已连接到服务端");
            String agentId = authResponse.getAgentId();
            context.setConnectionId(authResponse.getConnectionId());
            context.setAuthenticated(true);
            AgentType agentType = context.getAgentType();
            context.getAgentIdentity().updateIdentity(agentId, agentType.isStandalone());
            context.fireEvent(AgentEvent.AUTH_SUCCESS);
        } else if (code == 100) {
            String storagePath = context.getAgentIdentity().getStoragePath();
            logger.error("认证失败: {}，请删除本地身份文件后重试: {}", status.getMessage(), storagePath);
            context.fireEvent(AgentEvent.LOCAL_GOAWAY);
        } else {
            logger.error("{}", status.getMessage());
            context.fireEvent(AgentEvent.LOCAL_GOAWAY);
        }
    }
}
