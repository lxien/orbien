package io.github.lxien.orbien.client.statemachine.agent.action;

import io.github.lxien.orbien.client.manager.ProxyManager;
import io.github.lxien.orbien.client.manager.ProxyManagerHolder;
import io.github.lxien.orbien.client.statemachine.ContextConstants;
import io.github.lxien.orbien.client.statemachine.agent.AgentContext;
import io.github.lxien.orbien.client.statemachine.agent.AgentEvent;
import io.github.lxien.orbien.client.statemachine.agent.AgentState;
import io.github.lxien.orbien.core.message.Message;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.util.List;

public class ProxyReportRespAction extends AgentBaseAction {
    private final InternalLogger logger = InternalLoggerFactory.getInstance(ProxyReportRespAction.class);

    @Override
    protected void doExecute(AgentState from, AgentState to, AgentEvent event, AgentContext context) {
        Message.BatchCreateProxiesResponse response = context.getAndRemoveAs(
                ContextConstants.BATCH_CREATE_PROXIES_RESP,
                Message.BatchCreateProxiesResponse.class);
        List<Message.RuntimeInfo> itemsList = response.getItemsList();
        logger.debug("处理代理上报响应：{}", itemsList.size());
        ProxyManager proxyManager = ProxyManagerHolder.get();
        for (Message.RuntimeInfo p : itemsList) {
            proxyManager.add(p);
        }
    }
}