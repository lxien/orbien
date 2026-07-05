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

import com.google.protobuf.ProtocolStringList;
import io.github.lxien.orbien.common.utils.StringUtils;
import io.github.lxien.orbien.core.domain.DomainInfo;
import io.github.lxien.orbien.core.domain.ProxyConfig;
import io.github.lxien.orbien.core.domain.ProxyConfigExt;
import io.github.lxien.orbien.core.enums.DomainType;
import io.github.lxien.orbien.core.enums.ProtocolType;
import io.github.lxien.orbien.core.message.Message;
import io.github.lxien.orbien.core.message.support.RuntimeInfoSupport;
import io.github.lxien.orbien.core.notify.EventBus;
import io.github.lxien.orbien.server.config.AppConfig;
import io.github.lxien.orbien.server.event.ProxyAddEvent;
import io.github.lxien.orbien.server.event.ProxyDeleteEvent;
import io.github.lxien.orbien.server.exceptions.OrbienException;
import io.github.lxien.orbien.server.manager.ProxyManager;
import io.github.lxien.orbien.server.service.DomainConfigService;
import io.github.lxien.orbien.server.service.ProxyConfigService;
import io.github.lxien.orbien.server.statemachine.agent.AgentContext;
import io.github.lxien.orbien.server.uid.UidGenerator;
import io.github.lxien.orbien.server.vhost.DomainGenerator;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public abstract class AbstractHttpProxyProcessor implements ProxyProcessor {
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(AbstractHttpProxyProcessor.class);
    protected final AppConfig appConfig;
    protected final ProxyManager proxyManager;
    protected final DomainGenerator domainGenerator;
    protected final UidGenerator uidGenerator;
    protected final EventBus eventBus;
    protected final ProxyConfigService proxyConfigService;
    protected final DomainConfigService domainConfigService;

    public AbstractHttpProxyProcessor(
            AppConfig appConfig,
            ProxyManager proxyManager,
            DomainGenerator domainGenerator,
            UidGenerator uidGenerator,
            EventBus eventBus,
            ProxyConfigService proxyConfigService,
            DomainConfigService domainConfigService) {
        this.appConfig = appConfig;
        this.proxyManager = proxyManager;
        this.domainGenerator = domainGenerator;
        this.uidGenerator = uidGenerator;
        this.eventBus = eventBus;
        this.proxyConfigService = proxyConfigService;
        this.domainConfigService = domainConfigService;
    }

    @Override
    public Message.RuntimeInfo process(AgentContext context, Message.Proxy proxy) throws Exception {
        DomainType domainType = getDomainType(proxy.getDomain());
        List<DomainInfo> domains;
        String agentId = context.getAgentId();
        ProxyConfigExt ext = proxyConfigService.findByAgentAndName(agentId, proxy.getName());

        if (ext != null) {
            logger.debug("代理 {} 已存在，先删除旧的代理配置", proxy.getName());
            ProxyConfig config = ext.getProxyConfig();
            String proxyId = config.getProxyId();
            proxyManager.deactivate(proxyId);
            eventBus.publishSync(new ProxyDeleteEvent(agentId, proxyId));
        }
        Message.Domain domain = proxy.getDomain();
        if (domainType.isCustomDomain()) {
            domains = domainGenerator.generateCustomDomains(domain.getCustomDomainsList());
        } else if (domainType.isAuto()) {
            String rootDomain = randomBaseDomain();
            if (!StringUtils.hasText(rootDomain)) {
                throw new OrbienException("服务不支持自动生成域名");
            }
            DomainInfo domainInfo = domainGenerator.generateRandomSubdomain(rootDomain);
            domains = new ArrayList<>();
            domains.add(domainInfo);
        } else {
            String rootDomain = randomBaseDomain();
            if (!StringUtils.hasText(rootDomain)) {
                throw new OrbienException("系统不支持子域名");
            }
            ProtocolStringList subDomainsList = domain.getSubDomainsList();
            if (CollectionUtils.isEmpty(subDomainsList)) {
                throw new OrbienException("至少指定一个子域名");
            }
            domains = domainGenerator.generateSubdomains(rootDomain, subDomainsList);
        }

        String proxyId = uidGenerator.getUIDAsString();
        List<String> list = domains.stream().map(DomainInfo::getFullDomain).toList();
        HashSet<String> domainSet = new HashSet<>(list);

        doRegister(agentId, proxyId, domainSet);

        ProxyAddEvent proxyAddEvent = new ProxyAddEvent(agentId, proxyId, proxy, domains);
        eventBus.publishAsync(proxyAddEvent);

        Message.RuntimeInfo.Builder builder = Message.RuntimeInfo.newBuilder();
        builder.setProxyId(proxyId);
        builder.setName(proxy.getName());
        builder.setHealthCheck(proxy.getHealthCheck());
        builder.addAllTargets(proxy.getTargetsList());
        RuntimeInfoSupport.applyTransport(builder, proxy);

        for (String d : domainSet) {
            builder.addRemoteAddr(buildAddress(d));
        }
        logger.debug("代理 {} 注册成功", proxy.getName());
        return builder.build();
    }

    private String buildAddress(String domain) {
        ProtocolType protocolType = getProtocolType();
        String prefix;
        String port;

        switch (protocolType) {
            case HTTP:
                prefix = "http://";
                int httpPort = appConfig.getHttpProxyPort();
                port = httpPort == 80 ? "" : ":" + httpPort;
                break;
            case HTTPS:
                prefix = "https://";
                int httpsPort = appConfig.getHttpsProxyPort();
                port = httpsPort == 443 ? "" : ":" + httpsPort;
                break;
            default:
                prefix = "";
                port = "";
        }

        return prefix + domain + port;
    }

    protected abstract ProtocolType getProtocolType();

    protected abstract void doRegister(String agentId, String proxyId, Set<String> domains);

    private String randomBaseDomain() {
        List<String> allRootDomains = domainConfigService.findAllRootDomains();
        int index = ThreadLocalRandom.current().nextInt(allRootDomains.size());
        return allRootDomains.get(index);
    }

    public DomainType getDomainType(Message.Domain domain) {
        ProtocolStringList customDomains = domain.getCustomDomainsList();
        if (!customDomains.isEmpty()) {
            return DomainType.CUSTOM_DOMAIN;
        }
        ProtocolStringList subDomains = domain.getSubDomainsList();
        if (!subDomains.isEmpty()) {
            return DomainType.SUBDOMAIN;
        }
        if (domain.hasAutoDomain() && domain.getAutoDomain()) {
            return DomainType.AUTO;
        }
        return null;
    }
}
