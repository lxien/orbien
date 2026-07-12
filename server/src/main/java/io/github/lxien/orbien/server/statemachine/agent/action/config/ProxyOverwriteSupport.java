package io.github.lxien.orbien.server.statemachine.agent.action.config;

import io.github.lxien.orbien.core.domain.ProxyConfigExt;
import io.github.lxien.orbien.server.event.ProxyDeleteEvent;
import io.github.lxien.orbien.server.manager.ProxyManager;
import io.github.lxien.orbien.server.notify.EventBus;
import io.github.lxien.orbien.server.service.ProxyCacheEvictionService;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ProxyOverwriteSupport {
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(ProxyOverwriteSupport.class);

    @Autowired
    private ProxyManager proxyManager;
    @Autowired
    private EventBus eventBus;
    @Autowired
    private ProxyCacheEvictionService proxyCacheEvictionService;

    public String release(String agentId, ProxyConfigExt existing) {
        String proxyId = existing.getProxyConfig().getProxyId();
        logger.debug("代理 {} 覆盖回收 proxyId={}", existing.getProxyConfig().getName(), proxyId);
        proxyCacheEvictionService.evict(existing);
        proxyManager.deactivate(proxyId);
        eventBus.publishSync(new ProxyDeleteEvent(agentId, proxyId));
        return proxyId;
    }
}
