package io.github.lxien.orbien.server.event;

import io.github.lxien.orbien.server.notify.Event;
import io.github.lxien.orbien.server.statemachine.agent.AgentInfo;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AgentAuthEvent extends Event {
    /**
     * 客户端认证信息
     */
    private final AgentInfo agentInfo;
    /**
     * 是否是重连
     */
    private final boolean isReconnect;

}
