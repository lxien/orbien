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

import io.github.lxien.orbien.core.domain.ProxyConfigExt;
import io.github.lxien.orbien.core.enums.ProtocolType;
import io.github.lxien.orbien.core.message.Message;
import io.github.lxien.orbien.core.message.support.RuntimeInfoSupport;
import io.github.lxien.orbien.server.notify.EventBus;
import io.github.lxien.orbien.server.config.AppConfig;
import io.github.lxien.orbien.server.event.ProxyAddEvent;
import io.github.lxien.orbien.server.service.ProxyConfigService;
import io.github.lxien.orbien.server.statemachine.agent.AgentContext;
import io.github.lxien.orbien.server.uid.UidGenerator;
import io.github.lxien.orbien.server.manager.ProxyManager;
import io.github.lxien.orbien.core.enums.PortPoolType;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class TcpProxyProcessor implements ProxyProcessor {
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(TcpProxyProcessor.class);
    @Autowired
    private ProxyManager proxyManager;
    @Autowired
    private UidGenerator uidGenerator;
    @Autowired
    private ProxyConfigService proxyConfigService;
    @Autowired
    private EventBus eventBus;
    @Autowired
    private ListenPortResolver listenPortResolver;
    @Autowired
    private ProxyOverwriteSupport proxyOverwriteSupport;
    @Resource
    private AppConfig appConfig;

    @Override
    public Message.RuntimeInfo process(AgentContext context, Message.Proxy proxy) throws Exception {
        String agentId = context.getAgentId();
        int remotePort = proxy.getRemotePort();
        String name = proxy.getName();
        ProxyConfigExt existing = proxyConfigService.findByAgentAndName(agentId, name);
        String proxyId;
        if (existing != null) {
            proxyId = proxyOverwriteSupport.release(agentId, existing);
        } else {
            proxyId = uidGenerator.getUIDAsString();
        }
        Integer listenPort = listenPortResolver.resolve(remotePort, existing, PortPoolType.TCP);
        if (remotePort < 1 && (existing == null || existing.getProxyConfig().getListenPort() == null)) {
            logger.debug("TCP代理 {} 自动分配端口: {}", proxy.getName(), listenPort);
        }

        proxyManager.registerTcp(agentId, proxyId, listenPort);
        eventBus.publishSync(new ProxyAddEvent(agentId, proxyId, proxy, listenPort));

        Message.RuntimeInfo.Builder builder = Message.RuntimeInfo.newBuilder();
        builder.setProxyId(proxyId);
        builder.setName(proxy.getName());
        builder.setHealthCheck(proxy.getHealthCheck());
        builder.addAllTargets(proxy.getTargetsList());
        RuntimeInfoSupport.applyTransport(builder, proxy);
        builder.addRemoteAddr(appConfig.getServerAddr() + ":" + listenPort);
        logger.debug("TCP代理 {} 注册成功，监听端口: {}", proxy.getName(), listenPort);
        return builder.build();
    }

    @Override
    public boolean supports(Message.ProtocolType protocolType) {
        return Objects.requireNonNull(ProtocolType.fromName(protocolType.name())).isTcp();
    }
}
