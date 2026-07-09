package io.github.lxien.orbien.client.statemachine.agent.action;

import io.github.lxien.orbien.client.config.AppConfig;
import io.github.lxien.orbien.client.config.domain.AuthConfig;
import io.github.lxien.orbien.client.statemachine.agent.AgentContext;
import io.github.lxien.orbien.client.statemachine.agent.AgentEvent;
import io.github.lxien.orbien.client.statemachine.agent.AgentState;
import io.github.lxien.orbien.core.utils.StringUtils;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

public class ConfigCheckAction extends AgentBaseAction {
    private final InternalLogger logger = InternalLoggerFactory.getInstance(ConfigCheckAction.class);

    @Override
    protected void doExecute(AgentState from, AgentState to, AgentEvent event, AgentContext ctx) {
        logger.debug("检查客户端配置");
        AppConfig config = ctx.getConfig();
        AuthConfig auth = config.getAuthConfig();
        if (!StringUtils.hasText(auth.getToken())) {
            ctx.getStateMachine().fireEvent(ctx.getState(), AgentEvent.LOCAL_GOAWAY, ctx);
            logger.error("请配置身份认证密钥");
        } else {
            ctx.getStateMachine().fireEvent(ctx.getState(), AgentEvent.CONFIG_CHECKED, ctx);
        }
    }
}
