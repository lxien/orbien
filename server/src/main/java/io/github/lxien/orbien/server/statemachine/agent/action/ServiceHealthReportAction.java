package io.github.lxien.orbien.server.statemachine.agent.action;

import io.github.lxien.orbien.core.message.Message;
import io.github.lxien.orbien.server.loadbalance.HealthManager;
import io.github.lxien.orbien.server.statemachine.agent.AgentConstants;
import io.github.lxien.orbien.server.statemachine.agent.AgentContext;
import io.github.lxien.orbien.server.statemachine.agent.AgentEvent;
import io.github.lxien.orbien.server.statemachine.agent.AgentState;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;

@Component
public class ServiceHealthReportAction extends AgentBaseAction {
    private final InternalLogger logger = InternalLoggerFactory.getInstance(ServiceHealthReportAction.class);
    @Autowired
    private HealthManager healthManager;

    @Override
    protected void doExecute(AgentState from, AgentState to, AgentEvent event, AgentContext context) {
        Message.BatchReportServiceHealthRequest req = context.getAndRemoveAs(
                AgentConstants.BATCH_REPORT_SERVICE_HEALTH_REQUEST,
                Message.BatchReportServiceHealthRequest.class);
        if (req == null || CollectionUtils.isEmpty(req.getItemsList())) {
            logger.debug("收到空的健康状态上报，忽略");
            return;
        }

        List<Message.ServiceHealth> items = req.getItemsList();
        int updated = 0;
        for (Message.ServiceHealth item : items) {
            if (item.getStatus() == Message.HealthStatus.UNKNOWN) {
                continue;
            }
            logger.debug("更新代理服务健康：{},{}:{},{}", item.getProxyId(), item.getHost(), item.getPort(), item.getStatus());
            healthManager.updateHealth(item.getProxyId(), item.getHost(), item.getPort(), item.getStatus());
            updated++;
        }
        if (updated > 0) {
            logger.debug("批量更新健康状态完成，条数: {}", updated);
        }
    }
}
