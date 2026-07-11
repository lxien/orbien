/*
 *    Copyright 2026 lxien
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package io.github.lxien.orbien.client.statemachine.agent;

import com.alibaba.cola.statemachine.StateMachine;
import com.alibaba.cola.statemachine.StateMachineFactory;
import com.alibaba.cola.statemachine.builder.StateMachineBuilder;
import com.alibaba.cola.statemachine.builder.StateMachineBuilderFactory;
import io.github.lxien.orbien.client.statemachine.agent.action.*;
import io.github.lxien.orbien.client.statemachine.agent.action.*;
import io.github.lxien.orbien.client.statemachine.agent.action.*;
import io.github.lxien.orbien.client.statemachine.agent.action.*;
import io.github.lxien.orbien.client.statemachine.agent.action.connection.ConnPoolCreateAction;
import io.github.lxien.orbien.client.statemachine.agent.action.connection.NewConnectionCreateAction;

/**
 * 客户端代理状态机构建器
 * 负责构建和配置客户端代理的状态机，管理客户端的各种状态转换
 */
public class AgentStateMachineBuilder {

    /**
     * 状态机 ID
     */
    private static final String MACHINE_ID = "clientStateMachine";

    /**
     * 状态机持有者
     */
    private static class StateMachineHolder {
        /**
         * 状态机实例
         */
        private static final StateMachine<AgentState, AgentEvent, AgentContext> INSTANCE = build();

        /**
         * 构建状态机
         *
         * @return 构建好的状态机实例
         */
        private static StateMachine<AgentState, AgentEvent, AgentContext> build() {
            StateMachineBuilder<AgentState, AgentEvent, AgentContext> builder = StateMachineBuilderFactory.create();


            ConfigCheckAction configCheckAction = new ConfigCheckAction();
            TlslnitAction tlslnitAction = new TlslnitAction();
            ConnectAction connectAction = new ConnectAction();
            AuthAction authAction = new AuthAction();
            AuthRespAction authRespAction = new AuthRespAction();
            AuthSuccessAction authSuccessAction = new AuthSuccessAction();
            RuntimeConfigSyncAction proxySyncAction = new RuntimeConfigSyncAction();
            NetworkErrorAction networkErrorAction = new NetworkErrorAction();
            GoawayAction goawayAction = new GoawayAction();
            ConnPoolCreateAction createTunnelPoolAction = new ConnPoolCreateAction();
            ConnCreateRespAction connCreateRespAction = new ConnCreateRespAction();
            ProxyReportRespAction proxyReportRespAction = new ProxyReportRespAction();
            ErrorAction errorAction = new ErrorAction();
            NewConnectionCreateAction newConnectionCreateAction = new NewConnectionCreateAction();
            DisconnectedAction disconnectedAction = new DisconnectedAction();
            ConnRetryAction connRetryAction = new ConnRetryAction();

            builder.externalTransition()
                    .from(AgentState.IDLE)
                    .to(AgentState.CONNECTING)
                    .on(AgentEvent.START)
                    .perform(configCheckAction);

            // 配置检查
            builder.internalTransition()
                    .within(AgentState.CONNECTING)
                    .on(AgentEvent.CONFIG_CHECKED)
                    .perform(tlslnitAction);

            // SSL 初始化
            builder.internalTransition()
                    .within(AgentState.CONNECTING)
                    .on(AgentEvent.TLS_INITIALIZED)
                    .perform(connectAction);

            // TCP 连接成功
            builder.internalTransition()
                    .within(AgentState.CONNECTING)
                    .on(AgentEvent.CONNECT_SUCCESS)
                    .perform(authAction);

            // 认证响应
            builder.internalTransition()
                    .within(AgentState.CONNECTING)
                    .on(AgentEvent.AUTH_RESPONSE)
                    .perform(authRespAction);
            // 认证成功
            builder.externalTransition()
                    .from(AgentState.CONNECTING)
                    .to(AgentState.CONNECTED)
                    .on(AgentEvent.AUTH_SUCCESS)
                    .perform(authSuccessAction);

            builder.internalTransition()
                    .within(AgentState.CONNECTED)
                    .on(AgentEvent.PROXY_CONFIG_SYNC)
                    .perform(proxySyncAction);


            // 运行中出现网络错误
            builder.externalTransition()
                    .from(AgentState.CONNECTED)
                    .to(AgentState.DISCONNECTED)
                    .on(AgentEvent.NETWORK_ERROR)
                    .perform(networkErrorAction);
            // 断开连接
            builder.externalTransition()
                    .from(AgentState.CONNECTED)
                    .to(AgentState.DISCONNECTED)
                    .on(AgentEvent.DISCONNECT)
                    .perform(disconnectedAction);
            // 认证过程中断开，同样触发重连
            builder.externalTransition()
                    .from(AgentState.CONNECTING)
                    .to(AgentState.DISCONNECTED)
                    .on(AgentEvent.DISCONNECT)
                    .perform(disconnectedAction);
            // 连接断开后重连
            builder.externalTransition()
                    .from(AgentState.DISCONNECTED)
                    .to(AgentState.CONNECTING)
                    .on(AgentEvent.CONNECT_FAILURE)
                    .perform(connRetryAction);
            // 连接断开，尝试重连
            builder.externalTransitions()
                    .fromAmong(AgentState.DISCONNECTED)
                    .to(AgentState.CONNECTING)
                    .on(AgentEvent.RETRY)
                    .perform(connectAction);
            // 连接失败 尝试重连
            builder.internalTransition()
                    .within(AgentState.CONNECTING)
                    .on(AgentEvent.CONNECT_FAILURE)
                    .perform(connRetryAction);
            //首次连接失败 尝试重试
            builder.internalTransition()
                    .within(AgentState.CONNECTING)
                    .on(AgentEvent.RETRY)
                    .perform(connectAction);

            // 处理创建隧道池请求
            builder.internalTransition()
                    .within(AgentState.CONNECTED)
                    .on(AgentEvent.CREATE_CONN_POOL)
                    .perform(createTunnelPoolAction);

            // 处理隧道创建响应
            builder.internalTransition()
                    .within(AgentState.CONNECTED)
                    .on(AgentEvent.CREATE_CONN_POOL_RESP)
                    .perform(connCreateRespAction);

            // 处理代理创建响应
            builder.internalTransition()
                    .within(AgentState.CONNECTED)
                    .on(AgentEvent.PROXY_REPORT_RESP)
                    .perform(proxyReportRespAction);

            // 创建新连接
            builder.internalTransition()
                    .within(AgentState.CONNECTED)
                    .on(AgentEvent.CREATE_NEW_CONN)
                    .perform(newConnectionCreateAction);

            // 处理错误
            builder.internalTransition()
                    .within(AgentState.CONNECTED)
                    .on(AgentEvent.ERROR)
                    .perform(errorAction);

            // 本地停止客户端
            builder.externalTransitions()
                    .fromAmong(AgentState.IDLE, AgentState.CONNECTING,
                            AgentState.CONNECTED, AgentState.DISCONNECTED,
                            AgentState.FAILED)
                    .to(AgentState.SHUTDOWN)
                    .on(AgentEvent.LOCAL_GOAWAY)
                    .perform(goawayAction);

            // 来自远程停止客户端
            builder.externalTransitions()
                    .fromAmong(AgentState.CONNECTING, AgentState.CONNECTED,
                            AgentState.DISCONNECTED, AgentState.FAILED)
                    .to(AgentState.SHUTDOWN)
                    .on(AgentEvent.REMOTE_GOAWAY)
                    .perform(goawayAction);

            builder.build(MACHINE_ID);
            return StateMachineFactory.get(MACHINE_ID);
        }
    }

    /**
     * 获取状态机实例
     *
     * @return 状态机实例
     */
    public static StateMachine<AgentState, AgentEvent, AgentContext> getStateMachine() {
        return StateMachineHolder.INSTANCE;
    }
}