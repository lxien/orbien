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

import io.github.lxien.orbien.core.enums.ProtocolType;
import io.github.lxien.orbien.core.message.Message;
import io.github.lxien.orbien.server.notify.EventBus;
import io.github.lxien.orbien.server.config.AppConfig;
import io.github.lxien.orbien.server.service.DomainConfigService;
import io.github.lxien.orbien.server.uid.UidGenerator;
import io.github.lxien.orbien.server.manager.ProxyManager;
import io.github.lxien.orbien.server.service.ProxyConfigService;
import io.github.lxien.orbien.server.vhost.DomainGenerator;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Set;

@Component
public class HttpProxyProcessor extends AbstractHttpProxyProcessor {
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(HttpProxyProcessor.class);

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    public HttpProxyProcessor(AppConfig appConfig, ProxyManager proxyManager, DomainGenerator domainGenerator, UidGenerator uidGenerator,
                              EventBus eventBus, ProxyConfigService proxyConfigService,
                              DomainConfigService domainConfigService) {
        super(appConfig, proxyManager, domainGenerator, uidGenerator, eventBus, proxyConfigService, domainConfigService);
    }

    @Override
    protected void doRegister(String agentId, String proxyId, Set<String> domains) {
        proxyManager.registerHttp(agentId, proxyId, domains);
    }

    @Override
    protected ProtocolType getProtocolType() {
        return ProtocolType.HTTP;
    }


    @Override
    public boolean supports(Message.ProtocolType protocolType) {
        return Objects.requireNonNull(ProtocolType.fromName(protocolType.name())).isHttp();
    }
}
