package io.github.lxien.orbien.client.statemachine.agent.action.connection;

import io.github.lxien.orbien.client.config.AppConfig;
import io.github.lxien.orbien.client.statemachine.ContextConstants;
import io.github.lxien.orbien.client.statemachine.agent.AgentContext;
import io.github.lxien.orbien.client.statemachine.agent.AgentEvent;
import io.github.lxien.orbien.client.statemachine.agent.AgentState;
import io.github.lxien.orbien.client.statemachine.agent.action.AgentBaseAction;
import io.github.lxien.orbien.client.statemachine.agent.command.ConnCreateCommand;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

public class NewConnectionCreateAction extends AgentBaseAction {
    private final InternalLogger logger = InternalLoggerFactory.getInstance(NewConnectionCreateAction.class);

    @Override
    protected void doExecute(AgentState from, AgentState to, AgentEvent event, AgentContext context) {
        ConnCreateCommand command = context.getAndRemoveAs(ContextConstants.CREATE_CONN_COMMAND, ConnCreateCommand.class);
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
            ConnCreateHelper.createMultiplexTunnel(context, config, command.getProtocol(), command.isEncrypted());
        } else {
            int count = command.getEffectiveDirectCount();
            for (int i = 0; i < count; i++) {
                ConnCreateHelper.createDirectTunnel(context, config, command.getProtocol(), command.isEncrypted());
            }
        }
    }
}
