package com.xiaoniucode.etp.client.statemachine.agent.action;

import com.xiaoniucode.etp.client.config.AppConfig;
import com.xiaoniucode.etp.client.config.ConfigUtils;
import com.xiaoniucode.etp.client.health.HealthCheckHolder;
import com.xiaoniucode.etp.client.statemachine.ContextConstants;
import com.xiaoniucode.etp.client.statemachine.agent.AgentContext;
import com.xiaoniucode.etp.client.statemachine.agent.AgentEvent;
import com.xiaoniucode.etp.client.statemachine.agent.AgentState;
import com.xiaoniucode.etp.core.domain.ProxyConfig;
import com.xiaoniucode.etp.core.message.Message;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.util.List;

public class ProxyReportRespAction extends AgentBaseAction {
    private final InternalLogger logger = InternalLoggerFactory.getInstance(ProxyReportRespAction.class);

    // ANSI 颜色
    private static final String RESET = "\u001B[0m";
    private static final String GREEN = "\u001B[32m";
    private static final String CYAN = "\u001B[36m";
    private static final String YELLOW = "\u001B[33m";
    private static final String BOLD = "\u001B[1m";

    @Override
    protected void doExecute(AgentState from, AgentState to, AgentEvent event, AgentContext context) {
        Message.BatchCreateProxiesResponse response = context.getAndRemoveAs(
                ContextConstants.BATCH_CREATE_PROXIES_RESP,
                Message.BatchCreateProxiesResponse.class);
        List<Message.CreateProxyResponse> itemsList = response.getItemsList();
        for (Message.CreateProxyResponse proxy : itemsList) {

        }
        //启动健康检查
        startAllHealthChecks();
    }

    private void startAllHealthChecks() {
        AppConfig appConfig = ConfigUtils.getConfig();
        List<ProxyConfig> proxies = appConfig.getProxies();
        int enabledCount = 0;
        for (ProxyConfig proxy : proxies) {
            if (proxy.getHealthCheck() != null && proxy.getHealthCheck().isEnabled()) {
                HealthCheckHolder.get().startHealthCheck(proxy);
                enabledCount++;
            }
        }
        if (enabledCount > 0) {
            logger.debug("已启动 {} 个 Proxy 的健康检查任务", enabledCount);
        } else {
            logger.debug("当前没有任何代理配置开启健康检查，关闭健康检查管理器");
            HealthCheckHolder.get().shutdown();
        }
    }
}