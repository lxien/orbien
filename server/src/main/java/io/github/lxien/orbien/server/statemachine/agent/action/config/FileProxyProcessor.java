package io.github.lxien.orbien.server.statemachine.agent.action.config;

import io.github.lxien.orbien.core.enums.ProtocolType;
import io.github.lxien.orbien.core.message.Message;
import io.github.lxien.orbien.core.message.support.RuntimeInfoSupport;
import io.github.lxien.orbien.core.notify.EventBus;
import io.github.lxien.orbien.server.config.AppConfig;
import io.github.lxien.orbien.server.manager.ProxyManager;
import io.github.lxien.orbien.server.service.DomainConfigService;
import io.github.lxien.orbien.server.service.ProxyConfigService;
import io.github.lxien.orbien.server.uid.UidGenerator;
import io.github.lxien.orbien.server.vhost.DomainGenerator;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Set;

@Component
public class FileProxyProcessor extends AbstractHttpProxyProcessor {

    public FileProxyProcessor(AppConfig appConfig, ProxyManager proxyManager, DomainGenerator domainGenerator,
                              UidGenerator uidGenerator, EventBus eventBus, ProxyConfigService proxyConfigService,
                              DomainConfigService domainConfigService) {
        super(appConfig, proxyManager, domainGenerator, uidGenerator, eventBus, proxyConfigService, domainConfigService);
    }

    @Override
    protected void doRegister(String agentId, String proxyId, Set<String> domains) {
        proxyManager.registerFile(agentId, proxyId, domains);
    }

    @Override
    protected ProtocolType getProtocolType() {
        return ProtocolType.FILE;
    }

    @Override
    public boolean supports(Message.ProtocolType protocolType) {
        return Objects.requireNonNull(ProtocolType.fromName(protocolType.name())).isFile();
    }

    @Override
    public Message.RuntimeInfo process(io.github.lxien.orbien.server.statemachine.agent.AgentContext context,
                                       Message.Proxy proxy) throws Exception {
        Message.RuntimeInfo runtimeInfo = super.process(context, proxy);
        Message.RuntimeInfo.Builder builder = runtimeInfo.toBuilder();
        builder.clearTargets();
        if (proxy.hasFileLimits()) {
            builder.setFileLimits(proxy.getFileLimits());
        }
        RuntimeInfoSupport.applyTransport(builder, proxy);
        return builder.build();
    }
}
