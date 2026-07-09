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

package io.github.lxien.orbien.server.statemachine.agent.action;

import io.github.lxien.orbien.core.domain.ProxyConfig;
import io.github.lxien.orbien.core.message.Message;
import io.github.lxien.orbien.core.message.support.RuntimeInfoSupport;
import io.github.lxien.orbien.core.message.TMSP;
import io.github.lxien.orbien.core.message.TMSPFrame;
import io.github.lxien.orbien.core.domain.ProxyConfigExt;
import io.github.lxien.orbien.core.utils.ProtobufUtil;
import io.github.lxien.orbien.server.config.AppConfig;
import io.github.lxien.orbien.server.service.ProxyConfigService;
import io.github.lxien.orbien.server.service.ProxyRuntimeRegistry;
import io.github.lxien.orbien.server.statemachine.agent.AgentContext;
import io.github.lxien.orbien.server.statemachine.agent.AgentEvent;
import io.github.lxien.orbien.server.statemachine.agent.AgentState;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;

/**
 * 将代理配置信息同步给在线客户端
 */
@Component
public class ProxyConfigSyncAction extends AgentBaseAction {
    private final InternalLogger logger = InternalLoggerFactory.getInstance(ProxyConfigSyncAction.class);
    @Autowired
    private ProxyConfigService proxyConfigService;
    @Autowired
    private ProxyRuntimeRegistry proxyRuntimeRegistry;
    @Resource
    private AppConfig appConfig;

    @Override
    protected void doExecute(AgentState from, AgentState to, AgentEvent event, AgentContext context) {
        logger.debug("开始将代理配置运行信息同步到在线客户端");
        String agentId = context.getAgentId();
        List<ProxyConfigExt> proxies = proxyConfigService.findByAgentId(agentId);
        if (!CollectionUtils.isEmpty(proxies)) {
            List<Message.RuntimeInfo> list = proxies.stream()
                    .filter(p -> !p.getProxyConfig().isSocks5())
                    .map(p -> {
                        ProxyConfig config = p.getProxyConfig();
                        List<String> remoteAddrs = RuntimeInfoSupport.buildRemoteAddrs(
                                p.getDomains(),
                                config.getProtocol(),
                                appConfig.getHttpProxyPort(),
                                appConfig.getHttpsProxyPort());
                        return RuntimeInfoSupport.buildRuntimeInfo(config, remoteAddrs);
                    }).toList();
            Message.ProxySyncResponse.Builder builder = Message.ProxySyncResponse.newBuilder();
            builder.setProxySyncType(Message.ProxySyncType.FULL);
            builder.addAllItems(list);

            Channel control = context.getControl();
            ByteBuf payload = ProtobufUtil.toByteBuf(builder.build(), control.alloc());
            TMSPFrame frame = new TMSPFrame(0, TMSP.MSG_CONFIG_SYNC, payload);
            control.writeAndFlush(frame);
        }
        proxyRuntimeRegistry.registerByAgentId(agentId);
    }

}
