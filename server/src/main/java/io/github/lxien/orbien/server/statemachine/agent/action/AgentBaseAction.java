package io.github.lxien.orbien.server.statemachine.agent.action;

import com.alibaba.cola.statemachine.Action;
import io.github.lxien.orbien.server.statemachine.agent.AgentContext;
import io.github.lxien.orbien.server.statemachine.agent.AgentState;
import io.github.lxien.orbien.server.statemachine.agent.AgentEvent;

public abstract class AgentBaseAction implements Action<AgentState, AgentEvent, AgentContext> {

    @Override
    public final void execute(AgentState from, AgentState to, AgentEvent event, AgentContext context) {
        try {
            context.setState(to);
            doExecute(from, to, event, context);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected abstract void doExecute(AgentState from, AgentState to, AgentEvent event, AgentContext context);
}