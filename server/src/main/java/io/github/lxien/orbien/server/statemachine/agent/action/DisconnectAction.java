package io.github.lxien.orbien.server.statemachine.agent.action;

import io.github.lxien.orbien.core.domain.ProxyConfigExt;
import io.github.lxien.orbien.core.enums.AgentType;
import io.github.lxien.orbien.server.loadbalance.HealthManager;
import io.github.lxien.orbien.server.manager.ProxyManager;
import io.github.lxien.orbien.server.service.ProxyConfigService;
import io.github.lxien.orbien.server.statemachine.agent.*;
import io.github.lxien.orbien.server.statemachine.stream.StreamManager;
import io.github.lxien.orbien.server.transport.connection.DirectConnectionPool;
import io.github.lxien.orbien.server.transport.connection.MultiplexConnectionPool;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 控制连接断开事件处理。
 */
@Component
public class DisconnectAction extends AgentBaseAction {
    private final InternalLogger logger = InternalLoggerFactory.getInstance(DisconnectAction.class);

    @Autowired
    private ProxyConfigService proxyConfigService;
    @Autowired
    private HealthManager healthManager;
    @Autowired
    private AgentManager agentManager;
    @Autowired
    private DirectConnectionPool directConnectionPool;
    @Autowired
    private MultiplexConnectionPool multiplexConnectionPool;
    @Autowired
    private StreamManager streamManager;
    @Autowired
    private ProxyManager proxyManager;

    @Override
    protected void doExecute(AgentState from, AgentState to, AgentEvent event, AgentContext context) {
        String agentId = context.getAgentId();
        int proxyCount = 0;
        if (StringUtils.hasText(agentId)) {
            List<ProxyConfigExt> proxies = proxyConfigService.findByAgentId(agentId);
            proxyCount = proxies.size();
            for (ProxyConfigExt proxy : proxies) {
                if (proxy.getProxyConfig() != null) {
                    healthManager.removeProxy(proxy.getProxyConfig().getProxyId());
                }
            }
            logger.debug("客户端 {} 断连，已清除 {} 个代理健康状态", agentId, proxyCount);

            directConnectionPool.offline(agentId);
            multiplexConnectionPool.offline(agentId);
            streamManager.fireCloseByAgent(agentId);
        }

        agentManager.detachControlChannel(context);
        context.updateActiveTime();

        if (shouldGoawayImmediately(from, context)) {
            logger.debug("客户端 {} 断连后立即 Goaway, from={}", agentId, from);
            context.fireEvent(AgentEvent.LOCAL_GOAWAY);
        } else if (StringUtils.hasText(agentId)) {
            // STANDALONE 保留重连窗口，但暂停公网入口，避免路由到无控制通道的会话
            proxyManager.onAgentOffline(agentId);
            logger.debug("客户端 {} 断连，已暂停 {} 个代理的公网入口", agentId, proxyCount);
        }
    }

    private boolean shouldGoawayImmediately(AgentState from, AgentContext context) {
        if (from == AgentState.AUTHENTICATING) {
            return true;
        }
        AgentInfo agentInfo = context.getAgentInfo();
        if (agentInfo == null) {
            return true;
        }
        AgentType agentType = agentInfo.getAgentType();
        return agentType != null && agentType.isSession();
    }
}
