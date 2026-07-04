package io.github.lxien.orbien.client.statemachine.agent.action;

import com.alibaba.cola.statemachine.Action;
import io.github.lxien.orbien.client.statemachine.agent.AgentContext;
import io.github.lxien.orbien.client.statemachine.agent.AgentEvent;
import io.github.lxien.orbien.client.statemachine.agent.AgentState;

public abstract class AgentBaseAction implements Action<AgentState, AgentEvent, AgentContext> {
    @Override
    public void execute(AgentState from, AgentState to, AgentEvent event, AgentContext context) {
        try {
            context.setState(to);
            doExecute(from, to, event, context);
        }catch (Throwable e){
            e.printStackTrace();
        }

    }
    protected abstract void doExecute(AgentState from, AgentState to, AgentEvent event, AgentContext context);
}
