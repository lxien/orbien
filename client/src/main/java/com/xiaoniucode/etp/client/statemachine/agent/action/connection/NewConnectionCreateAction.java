package com.xiaoniucode.etp.client.statemachine.agent.action.connection;

import com.xiaoniucode.etp.client.config.AppConfig;
import com.xiaoniucode.etp.client.statemachine.ContextConstants;
import com.xiaoniucode.etp.client.statemachine.agent.AgentContext;
import com.xiaoniucode.etp.client.statemachine.agent.AgentEvent;
import com.xiaoniucode.etp.client.statemachine.agent.AgentState;
import com.xiaoniucode.etp.client.statemachine.agent.action.AgentBaseAction;
import com.xiaoniucode.etp.client.statemachine.agent.command.ConnCreateCommand;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

/**
 * 创建新的连接
 * 支持多路复用连接和独立连接，独立连接支持自定义批量创建
 */
public class NewConnectionCreateAction extends AgentBaseAction {
    private final InternalLogger logger = InternalLoggerFactory.getInstance(NewConnectionCreateAction.class);

    @Override
    protected void doExecute(AgentState from, AgentState to, AgentEvent event, AgentContext context) {
        ConnCreateCommand command = context.getAndRemoveAs(ContextConstants.CREATE_CONN_COMMAND,
                ConnCreateCommand.class);

        if (command == null) {
            logger.error("创建连接命令参数为空");
            return;
        }

        String validateResult = command.validate();
        if (validateResult != null) {
            logger.error("创建连接参数校验失败：{}", validateResult);
            return;
        }

        AppConfig config = context.getConfig();

        if (command.isMultiplex()) {
            ConnCreateHelper.createMultiplexTunnel(context, config, command.isEncrypted());
        } else {
            int count = command.getEffectiveDirectCount();
            for (int i = 0; i < count; i++) {
                ConnCreateHelper.createDirectTunnel(context, config, command.isEncrypted());
            }
        }
    }
}
