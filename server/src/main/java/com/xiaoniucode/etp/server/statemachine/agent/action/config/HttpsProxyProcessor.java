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

package com.xiaoniucode.etp.server.statemachine.agent.action.config;

import com.xiaoniucode.etp.core.enums.ProtocolType;
import com.xiaoniucode.etp.core.message.Message;
import com.xiaoniucode.etp.core.notify.EventBus;
import com.xiaoniucode.etp.server.config.AppConfig;
import com.xiaoniucode.etp.server.manager.ProxyManager;
import com.xiaoniucode.etp.server.service.DomainConfigService;
import com.xiaoniucode.etp.server.service.ProxyConfigService;
import com.xiaoniucode.etp.server.uid.UidGenerator;
import com.xiaoniucode.etp.server.vhost.DomainGenerator;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Set;

@Component
public class HttpsProxyProcessor extends AbstractHttpProxyProcessor {
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    public HttpsProxyProcessor(AppConfig appConfig, ProxyManager proxyManager, DomainGenerator domainGenerator,
                               UidGenerator uidGenerator, EventBus eventBus,
                               ProxyConfigService proxyConfigService, DomainConfigService domainConfigService) {
        super(appConfig, proxyManager, domainGenerator, uidGenerator, eventBus, proxyConfigService, domainConfigService);
    }

    @Override
    protected void doRegister(String agentId, String proxyId, Set<String> domains) {
        proxyManager.registerHttps(agentId, proxyId, domains);
    }

    @Override
    protected ProtocolType getProtocolType() {
        return ProtocolType.HTTPS;
    }
    @Override
    public boolean supports(Message.ProtocolType protocolType) {
        return Objects.requireNonNull(ProtocolType.fromName(protocolType.name())).isHttps();
    }
}
