/*
 *
 *  *    Copyright 2026 xiaoniucode
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

package com.xiaoniucode.etp.client.statemachine.agent.action;


import com.xiaoniucode.etp.client.config.AppConfig;
import com.xiaoniucode.etp.client.config.ConfigUtils;
import com.xiaoniucode.etp.client.manager.ProxyManager;
import com.xiaoniucode.etp.client.manager.ProxyManagerHolder;
import com.xiaoniucode.etp.client.statemachine.ContextConstants;
import com.xiaoniucode.etp.client.statemachine.agent.AgentContext;
import com.xiaoniucode.etp.client.statemachine.agent.AgentEvent;
import com.xiaoniucode.etp.client.statemachine.agent.AgentState;
import com.xiaoniucode.etp.client.utils.ProxyConfigAssembler;
import com.xiaoniucode.etp.core.domain.ProxyConfig;
import com.xiaoniucode.etp.core.domain.ProxyConfigExt;
import com.xiaoniucode.etp.core.message.Message;
import com.xiaoniucode.etp.core.message.TMSP;
import com.xiaoniucode.etp.core.message.TMSPFrame;
import com.xiaoniucode.etp.core.utils.ProtobufUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.util.List;

/**
 * 处理来自服务端推送的代理配置信息
 */
public class ProxySyncAction extends AgentBaseAction {
    private final InternalLogger logger = InternalLoggerFactory.getInstance(ProxySyncAction.class);

    @Override
    protected void doExecute(AgentState from, AgentState to, AgentEvent event, AgentContext context) {
        Message.ProxySyncResponse res = context.getAndRemoveAs(ContextConstants.PROXY_SYNC_RESP,
                Message.ProxySyncResponse.class);
        if (res == null) return;
        ProxyManager proxyManager = ProxyManagerHolder.get();
        Message.ProxySyncType syncType = res.getProxySyncType();
        switch (syncType) {
            case FULL: {
                List<Message.ProxyRuntime> itemsList = res.getItemsList();
                List<ProxyConfigExt> list = itemsList.stream()
                        .map(ProxyConfigAssembler::toDomain).toList();
                proxyManager.apply(list);
                //处理本地配置推送
                handleLocalProxyPush(context, proxyManager);
                break;
            }
            case ADD: {
                List<Message.ProxyRuntime> itemsList = res.getItemsList();
                List<ProxyConfigExt> list = itemsList.stream()
                        .map(ProxyConfigAssembler::toDomain).toList();
                proxyManager.batchAdd(list);
                break;
            }
            case UPDATE: {
                List<Message.ProxyRuntime> itemsList = res.getItemsList();
                List<ProxyConfigExt> list = itemsList.stream()
                        .map(ProxyConfigAssembler::toDomain).toList();
                proxyManager.batchUpdate(list);
                break;
            }
            case DELETE: {
                List<String> proxyIds = res.getProxyIdsList().stream().toList();
                proxyManager.batchDelete(proxyIds);
                break;
            }
        }
    }

    private void handleLocalProxyPush(AgentContext context, ProxyManager proxyManager) {
        logger.debug("推送本地配置到服务端");
        AppConfig config = ConfigUtils.getConfig();
        List<ProxyConfig> proxies = config.getProxies();
        List<ProxyConfig> list = proxies.stream().filter(c ->
                !proxyManager.nameExists(c.getName())).toList();

        Channel control = context.getControl();

        Message.BatchCreateProxiesRequest.Builder builder = Message.BatchCreateProxiesRequest.newBuilder();
        List<Message.Proxy> messages = list.stream().map(ProxyConfigAssembler::toProto).toList();
        builder.addAllProxies(messages);

        ByteBuf buf = ProtobufUtil.toByteBuf(builder.build(), control.alloc());
        TMSPFrame tmspFrame = new TMSPFrame(0, TMSP.MSG_PROXY_REPORT_REQ, buf);
        control.writeAndFlush(tmspFrame).addListener(future -> {
            if (future.isSuccess()) {
                logger.debug("成功推送 {} 条代理配置到服务端", messages.size());
            }
        });
    }
}
