package io.github.lxien.orbien.server.statemachine.agent.action;

import io.github.lxien.orbien.core.domain.ProxyConfigExt;
import io.github.lxien.orbien.core.enums.AgentType;
import io.github.lxien.orbien.server.loadbalance.HealthManager;
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
 * 控制连接断开时的统一处理：健康状态清理、连接索引解除、运行时资源下线
 * 会话型客户端及认证中断连立即进入 Goaway 终态；标准客户端保留重连窗口
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

            directConnectionPool.offline(agentId);
            multiplexConnectionPool.offline(agentId);
            streamManager.fireCloseByAgent(agentId);
        }

        agentManager.detachControlChannel(context);
        context.updateActiveTime();

        if (shouldGoawayImmediately(from, context)) {
            logger.debug("客户端 {} 断连后立即清理（from={}, type={}）",
                    agentId, from, context.getAgentInfo() != null ? context.getAgentInfo().getAgentType() : null);
            context.fireEvent(AgentEvent.LOCAL_GOAWAY);
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
