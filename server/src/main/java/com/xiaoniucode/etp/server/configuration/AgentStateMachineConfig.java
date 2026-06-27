package com.xiaoniucode.etp.server.configuration;

import com.alibaba.cola.statemachine.StateMachine;
import com.alibaba.cola.statemachine.builder.StateMachineBuilder;
import com.alibaba.cola.statemachine.builder.StateMachineBuilderFactory;
import com.xiaoniucode.etp.server.statemachine.agent.AgentContext;
import com.xiaoniucode.etp.server.statemachine.agent.AgentEvent;
import com.xiaoniucode.etp.server.statemachine.agent.AgentState;
import com.xiaoniucode.etp.server.statemachine.agent.action.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AgentStateMachineConfig {
    @Autowired
    private AuthAction authAction;
    @Autowired
    private AgentInitAction agentInitAction;
    @Autowired
    private ConfigSyncAction configSyncAction;
    @Autowired
    private ProxyReportAction proxyReportAction;
    @Autowired
    private CreateConnectionAction createConnectionAction;
    @Autowired
    private GoawayAction goawayAction;
    @Autowired
    private AuthFailureAction authFailureAction;
    @Autowired
    private HeartbeatTimeoutAction heartbeatTimeoutAction;
    @Autowired
    private RetryTimeoutAction retryTimeoutAction;

    @Bean("agentStateMachine")
    public StateMachine<AgentState, AgentEvent, AgentContext> createStateMachine() {
        StateMachineBuilder<AgentState, AgentEvent, AgentContext> builder = StateMachineBuilderFactory.create();

        // 新连接建立
        builder.externalTransition()
                .from(AgentState.NEW)
                .to(AgentState.AUTHENTICATING)
                .on(AgentEvent.AUTH_START)
                .when(ctx -> true)
                .perform(authAction);

        // 认证成功-同步代理配置
        builder.externalTransition()
                .from(AgentState.AUTHENTICATING)
                .to(AgentState.CONNECTED)
                .on(AgentEvent.AUTH_SUCCESS)
                .when(ctx -> true)
                .perform(configSyncAction);
        //初始化客户端运行时信息
        builder.internalTransition()
                .within(AgentState.CONNECTED)
                .on(AgentEvent.AGENT_INIT)
                .when(ctx -> true)
                .perform(agentInitAction);
        // 认证失败
        builder.externalTransition()
                .from(AgentState.AUTHENTICATING)
                .to(AgentState.FAILED)
                .on(AgentEvent.AUTH_FAILURE)
                .when(ctx -> true)
                .perform(authFailureAction);

        // 处理代理创建请求
        builder.internalTransition()
                .within(AgentState.CONNECTED)
                .on(AgentEvent.PROXY_REPORT)
                .when(ctx -> true)
                .perform(proxyReportAction);

        // 创建隧道
        builder.internalTransition()
                .within(AgentState.CONNECTED)
                .on(AgentEvent.CREATE_TUNNEL)
                .when(ctx -> true)
                .perform(createConnectionAction);

        // 网络断开
        builder.externalTransition()
                .from(AgentState.CONNECTED)
                .to(AgentState.DISCONNECTED)
                .on(AgentEvent.DISCONNECT)
                .when(ctx -> true)
                .perform((from, to, event, context) -> context.setState(to));

        // 心跳超时
        builder.externalTransition()
                .from(AgentState.CONNECTED)
                .to(AgentState.DISCONNECTED)
                .on(AgentEvent.HEARTBEAT_TIMEOUT)
                .when(ctx -> true)
                .perform(heartbeatTimeoutAction);

        // 认证中断开
        builder.externalTransition()
                .from(AgentState.AUTHENTICATING)
                .to(AgentState.DISCONNECTED)
                .on(AgentEvent.DISCONNECT)
                .when(ctx -> true)
                .perform((from, to, event, context) -> context.setState(to));

        // 本地停止客户端
        builder.externalTransitions()
                .fromAmong(AgentState.NEW,
                        AgentState.AUTHENTICATING,
                        AgentState.CONNECTED,
                        AgentState.DISCONNECTED)
                .to(AgentState.CLOSED)
                .on(AgentEvent.LOCAL_GOAWAY)
                .when(ctx -> true)
                .perform(goawayAction);

        // 来自远程主动停止
        builder.externalTransitions()
                .fromAmong(AgentState.NEW,
                        AgentState.AUTHENTICATING,
                        AgentState.CONNECTED,
                        AgentState.DISCONNECTED)
                .to(AgentState.CLOSED)
                .on(AgentEvent.REMOTE_GOAWAY)
                .when(ctx -> true)
                .perform(goawayAction);

        // 重连窗口超时
        builder.externalTransition()
                .from(AgentState.DISCONNECTED)
                .to(AgentState.FAILED)
                .on(AgentEvent.RETRY_TIMEOUT)
                .when(ctx -> true)
                .perform(retryTimeoutAction);
        // 重连
        builder.externalTransition()
                .from(AgentState.DISCONNECTED)
                .to(AgentState.AUTHENTICATING)
                .on(AgentEvent.RETRY_CONNECT)
                .when(ctx -> true)
                .perform(authAction);
        return builder.build("agent-state-machine");
    }
}