package io.github.lxien.orbien.client.statemachine.agent.action;

import io.github.lxien.orbien.client.statemachine.agent.AgentContext;
import io.github.lxien.orbien.client.statemachine.agent.AgentEvent;
import io.github.lxien.orbien.client.statemachine.agent.AgentState;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

/**
 * 网络错误处理
 */
public class NetworkErrorAction extends AgentBaseAction {
    private final InternalLogger logger= InternalLoggerFactory.getInstance(NetworkErrorAction.class);
    @Override
    protected void doExecute(AgentState from, AgentState to, AgentEvent event, AgentContext context) {
        if (context.isShuttingDown()) {
            return;
        }
        logger.error("网络错误");
        context.fireEvent(AgentEvent.RETRY);
    }
}
