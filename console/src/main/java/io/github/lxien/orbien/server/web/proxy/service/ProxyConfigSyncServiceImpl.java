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

package io.github.lxien.orbien.server.web.proxy.service;

import io.github.lxien.orbien.core.message.Message;
import io.github.lxien.orbien.core.message.TMSP;
import io.github.lxien.orbien.core.message.TMSPFrame;
import io.github.lxien.orbien.core.utils.ProtobufUtil;
import io.github.lxien.orbien.server.statemachine.agent.AgentManager;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 将管理面板变更的代理配置信息推送给在线边缘代理客户端
 */
@Service
public class ProxyConfigSyncServiceImpl implements ProxyConfigSyncService {
    private final Logger logger = LoggerFactory.getLogger(ProxyConfigSyncServiceImpl.class);
    @Autowired
    private AgentManager agentManager;

    @Override
    public void syncOnCreate(String agentId, Message.RuntimeInfo runtimeInfo) {
        sendSyncMessage(agentId, Message.ProxySyncType.ADD, builder -> builder.addItems(runtimeInfo), runtimeInfo, "添加运行时配置");
    }

    @Override
    public void syncOnUpdate(String agentId, Message.RuntimeInfo runtimeInfo) {
        sendSyncMessage(agentId, Message.ProxySyncType.UPDATE, builder -> builder.addItems(runtimeInfo), runtimeInfo, "更新运行时配置");
    }

    @Override
    public void syncOnDelete(String agentId, String proxyId) {
        sendSyncMessage(agentId, Message.ProxySyncType.DELETE, builder -> builder.addProxyIds(proxyId), proxyId, "删除运行时配置");
    }

    @Override
    public void syncOnBatchDelete(String agentId, List<String> proxyIds) {
        sendSyncMessage(agentId, Message.ProxySyncType.DELETE, builder -> builder.addAllProxyIds(proxyIds), proxyIds, "批量删除运行时配置");
    }

    private void sendSyncMessage(String agentId, Message.ProxySyncType syncType,
                                  java.util.function.Consumer<Message.ProxySyncResponse.Builder> dataConsumer,
                                  Object logData, String logPrefix) {
        agentManager.getAgentContext(agentId).ifPresent(context -> {
            Channel control = context.getControl();
            control.eventLoop().execute(() -> {
                Message.ProxySyncResponse.Builder builder = Message.ProxySyncResponse.newBuilder();
                builder.setProxySyncType(syncType);
                dataConsumer.accept(builder);

                ByteBuf byteBuf = ProtobufUtil.toByteBuf(builder.build(), control.alloc());
                TMSPFrame frame = new TMSPFrame(0, TMSP.MSG_CONFIG_SYNC, byteBuf);
                control.writeAndFlush(frame);
                logger.info("{}：{}", logPrefix, logData);
            });
        });
    }
}
