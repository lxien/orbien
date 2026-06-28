package com.xiaoniucode.etp.client.statemachine.agent.action;

import com.xiaoniucode.etp.client.manager.ProxyManager;
import com.xiaoniucode.etp.client.manager.ProxyManagerHolder;
import com.xiaoniucode.etp.client.statemachine.ContextConstants;
import com.xiaoniucode.etp.client.statemachine.agent.AgentContext;
import com.xiaoniucode.etp.client.statemachine.agent.AgentEvent;
import com.xiaoniucode.etp.client.statemachine.agent.AgentState;
import com.xiaoniucode.etp.client.utils.ProxyConfigAssembler;
import com.xiaoniucode.etp.core.domain.ProxyConfigExt;
import com.xiaoniucode.etp.core.message.Message;
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
        List<Message.ProxyRuntime> itemsList = response.getItemsList();

        logger.debug("处理代理上报响应：{}", itemsList.size());
        ProxyManager proxyManager = ProxyManagerHolder.get();
        for (Message.ProxyRuntime p : itemsList) {
            ProxyConfigExt config = ProxyConfigAssembler.toDomain(p);
            proxyManager.add(config);
        }
    }
}