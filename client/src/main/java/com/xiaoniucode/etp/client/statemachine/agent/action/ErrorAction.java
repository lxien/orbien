package com.xiaoniucode.etp.client.statemachine.agent.action;

import com.xiaoniucode.etp.client.statemachine.agent.AgentContext;
import com.xiaoniucode.etp.client.statemachine.agent.AgentEvent;
import com.xiaoniucode.etp.client.statemachine.agent.AgentState;
import com.xiaoniucode.etp.core.message.Message;
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
