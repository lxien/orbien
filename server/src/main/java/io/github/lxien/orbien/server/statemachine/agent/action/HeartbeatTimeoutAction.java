package io.github.lxien.orbien.server.statemachine.agent.action;

import io.github.lxien.orbien.server.statemachine.agent.AgentContext;
import io.github.lxien.orbien.server.statemachine.agent.AgentEvent;
import io.github.lxien.orbien.server.statemachine.agent.AgentState;
import org.springframework.stereotype.Component;

@Component
public class HeartbeatTimeoutAction  extends AgentBaseAction{
    @Override
    protected void doExecute(AgentState from, AgentState to, AgentEvent event, AgentContext context) {

    }
}
