package io.github.lxien.orbien.server.statemachine.agent.action;

import io.github.lxien.orbien.core.domain.ProxyConfigExt;
import io.github.lxien.orbien.server.loadbalance.HealthManager;
import io.github.lxien.orbien.server.service.ProxyConfigService;
import io.github.lxien.orbien.server.statemachine.agent.AgentContext;
import io.github.lxien.orbien.server.statemachine.agent.AgentEvent;
import io.github.lxien.orbien.server.statemachine.agent.AgentState;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 客户端断连时清理该 Agent 下所有代理的运行时健康状态，避免得到过期结果。
 */
@Component
public class AgentDisconnectHealthCleanupAction extends AgentBaseAction {
    private final InternalLogger logger = InternalLoggerFactory.getInstance(AgentDisconnectHealthCleanupAction.class);

    @Autowired
    private ProxyConfigService proxyConfigService;
    @Autowired
    private HealthManager healthManager;

    @Override
    protected void doExecute(AgentState from, AgentState to, AgentEvent event, AgentContext context) {
        String agentId = context.getAgentId();
        if (StringUtils.hasText(agentId)) {
            List<ProxyConfigExt> proxies = proxyConfigService.findByAgentId(agentId);
            for (ProxyConfigExt proxy : proxies) {
                if (proxy.getProxyConfig() != null) {
                    healthManager.removeProxy(proxy.getProxyConfig().getProxyId());
                }
            }
            logger.debug("客户端 {} 断连，已清除 {} 个代理的运行时健康状态", agentId, proxies.size());
        }
    }
}
