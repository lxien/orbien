/*
 *
 *  *    Copyright 2026 lxien
 *  *
 *  *    Licensed under the Apache License, Version 2.0 (the "License");
 *  *    you may not use this file except in compliance with the License.
 *  *    You may obtain a copy of the License at
 *  *
 *  *        http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  *    Unless required by applicable law or agreed to in writing, software
 *  *    distributed under the License is distributed on an "AS IS" BASIS,
 *  *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  *    See the License for the specific language governing permissions and
 *  *    limitations under the License.
 *
 */

package io.github.lxien.orbien.server.statemachine.agent.action.config;

import io.github.lxien.orbien.core.message.Message;
import io.github.lxien.orbien.core.message.TMSP;
import io.github.lxien.orbien.core.message.TMSPFrame;
import io.github.lxien.orbien.core.utils.ProtobufUtil;
import io.github.lxien.orbien.server.statemachine.agent.*;
import io.github.lxien.orbien.server.statemachine.agent.AgentConstants;
import io.github.lxien.orbien.server.statemachine.agent.AgentContext;
import io.github.lxien.orbien.server.statemachine.agent.AgentEvent;
import io.github.lxien.orbien.server.statemachine.agent.AgentState;
import io.github.lxien.orbien.server.statemachine.agent.action.AgentBaseAction;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.*;


@Component
public class ProxyReportAction extends AgentBaseAction {
    private final InternalLogger logger = InternalLoggerFactory.getInstance(ProxyReportAction.class);

    @Autowired
    private ProxyProcessorFactory proxyProcessorFactory;

    @Override
    protected void doExecute(AgentState from, AgentState to, AgentEvent event, AgentContext context) {
        Channel control = context.getControl();

        Message.BatchCreateProxiesRequest proxies = context.getAndRemoveAs(
                AgentConstants.BATCH_CREATE_PROXIES_REQUEST, Message.BatchCreateProxiesRequest.class);
        if (proxies == null) return;

        List<Message.Proxy> proxiesList = proxies.getProxiesList();
        logger.info("收到客户端代理配置上报，共 {} 条: {}",
                proxiesList.size(),
                proxiesList.stream().map(Message.Proxy::getName).toList());

        Message.BatchCreateProxiesResponse.Builder builder = Message.BatchCreateProxiesResponse.newBuilder();
        for (Message.Proxy proxy : proxiesList) {
            try {
                ProxyProcessor processor = proxyProcessorFactory.getProcessor(proxy.getProtocol());
                Message.RuntimeInfo runtimeInfo = processor.process(context, proxy);
                builder.addItems(runtimeInfo);
            } catch (Exception e) {
                logger.error("代理配置处理失败", e);
                sendErrorResponse(e, control);
            }
        }
        builder.setStatus(Message.Status.newBuilder().setCode(0).build());
        ByteBuf payload = ProtobufUtil.toByteBuf(builder.build(), control.alloc());
        TMSPFrame frame = new TMSPFrame(0, TMSP.MSG_PROXY_REPORT_RESP, payload);

        control.writeAndFlush(frame).addListener((ChannelFutureListener) future -> {
            if (!future.isSuccess()) {
                logger.error(future.cause());
            }
        });
        logger.debug("成功上报配置成功");

    }

    private void sendErrorResponse(Exception e, Channel control) {
        String message = e.getMessage();
        if (!StringUtils.hasText(message)) {
            Throwable cause = e.getCause();
            if (cause != null && StringUtils.hasText(cause.getMessage())) {
                message = cause.getMessage();
            }
        } else {
            message = "代理配置推送失败，未知错误";
        }
        Message.Error error = Message.Error.newBuilder()
                .setStatus(Message.Status.newBuilder().setCode(1).setMessage(message)).build();

        ByteBuf payload = ProtobufUtil.toByteBuf(error, control.alloc());
        TMSPFrame frame = new TMSPFrame(TMSP.MSG_ERROR, payload);
        control.writeAndFlush(frame);
        logger.error(message);
    }
}
