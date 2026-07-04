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
package io.github.lxien.orbien.client.statemachine.agent.action;


import io.github.lxien.orbien.client.config.AppConfig;
import io.github.lxien.orbien.client.health.HealthCheckHolder;
import io.github.lxien.orbien.client.statemachine.agent.AgentContext;
import io.github.lxien.orbien.client.statemachine.agent.AgentEvent;
import io.github.lxien.orbien.client.statemachine.agent.AgentState;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

/**
 * 与服务端建立连接
 */
public class ConnectAction extends AgentBaseAction {
    private final InternalLogger logger = InternalLoggerFactory.getInstance(ConnectAction.class);

    @Override
    protected void doExecute(AgentState from, AgentState to, AgentEvent event, AgentContext ctx) {
        try {
            AppConfig appConfig = ctx.getConfig();
            Bootstrap controlBootstrap = ctx.getControlBootstrap();
            ChannelFuture channelFuture = controlBootstrap.connect(appConfig.getServerAddr(), appConfig.getServerPort());
            channelFuture.addListener((ChannelFutureListener) f -> {
                if (f.isSuccess()) {
                    logger.debug("成功连接到服务器：{}:{}",appConfig.getServerAddr(),appConfig.getServerPort());
                    Channel control = channelFuture.channel();
                    ctx.getRetryCount().set(0);
                    ctx.setControl(control);
                    HealthCheckHolder.init(control);
                    ctx.fireEvent(AgentEvent.CONNECT_SUCCESS);
                } else {
                    logger.error("无法连接到代理服务器：{}:{}",appConfig.getServerAddr(),appConfig.getServerPort());
                    ctx.fireEvent(AgentEvent.CONNECT_FAILURE);
                }
            });
        } catch (Exception e) {
            ctx.fireEvent(AgentEvent.CONNECT_FAILURE);
        }
    }
}