package io.github.lxien.orbien.client.statemachine.agent.action;

import io.github.lxien.orbien.client.statemachine.agent.AgentContext;
import io.github.lxien.orbien.client.statemachine.agent.AgentEvent;
import io.github.lxien.orbien.client.statemachine.agent.AgentState;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

public class ErrorAction extends AgentBaseAction{
    private final InternalLogger logger= InternalLoggerFactory.getInstance(ErrorAction.class);
    @Override
    protected void doExecute(AgentState from, AgentState to, AgentEvent event, AgentContext context) {
//    todo     Message.Error error = context.getAndRemoveAs("ERROR", Message.Error.class);
//        logger.error(error.getMessage());
    }
}
