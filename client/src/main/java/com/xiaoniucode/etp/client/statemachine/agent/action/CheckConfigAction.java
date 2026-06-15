package com.xiaoniucode.etp.client.statemachine.agent.action;

import com.xiaoniucode.etp.client.config.AppConfig;
import com.xiaoniucode.etp.client.config.domain.AuthConfig;
import com.xiaoniucode.etp.client.statemachine.agent.AgentContext;
import com.xiaoniucode.etp.client.statemachine.agent.AgentEvent;
import com.xiaoniucode.etp.client.statemachine.agent.AgentState;
import com.xiaoniucode.etp.common.utils.StringUtils;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

public class CheckConfigAction extends AgentBaseAction {
    private final InternalLogger logger = InternalLoggerFactory.getInstance(CheckConfigAction.class);

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
