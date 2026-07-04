package io.github.lxien.orbien.server.statemachine.agent;

import com.alibaba.cola.statemachine.StateMachine;
import io.github.lxien.orbien.core.transport.AbstractAgentContext;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class AgentContext extends AbstractAgentContext {
    private AgentState state = AgentState.NEW;
    private AgentInfo agentInfo;
    private StateMachine<AgentState, AgentEvent, AgentContext> stateMachine;

    public AgentContext(StateMachine<AgentState, AgentEvent, AgentContext> agentStateMachine) {
        stateMachine = agentStateMachine;
        updateActiveTime();
    }

    public String getAgentId() {
        if (agentInfo != null) {
            return agentInfo.getAgentId();
        }
        return null;
    }

    public void fireEvent(AgentEvent event) {
        stateMachine.fireEvent(state, event, this);
    }

    @Override
    public void updateActiveTime() {
        super.updateActiveTime();
        if (agentInfo != null) {
            agentInfo.setLastActiveTime(this.getLastActiveTime());
        }
    }
}
