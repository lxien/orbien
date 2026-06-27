/*
 *    Copyright 2026 xiaoniucode
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package com.xiaoniucode.etp.client.statemachine.agent.action;

import com.xiaoniucode.etp.client.config.domain.ConnectionConfig;
import com.xiaoniucode.etp.client.config.domain.RetryConfig;
import com.xiaoniucode.etp.client.statemachine.agent.AgentContext;
import com.xiaoniucode.etp.client.statemachine.agent.AgentEvent;
import com.xiaoniucode.etp.client.statemachine.agent.AgentState;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.util.concurrent.TimeUnit;

public class ConnRetryAction extends AgentBaseAction {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(ConnRetryAction.class);

    @Override
    protected void doExecute(AgentState from, AgentState to, AgentEvent event, AgentContext ctx) {
        ConnectionConfig connectionConfig = ctx.getConfig().getConnectionConfig();
        RetryConfig retryConfig = connectionConfig.getRetryConfig();
        int currentRetry = ctx.getRetryCount().incrementAndGet();
        if (currentRetry > retryConfig.getMaxRetries()) {
            logger.warn("已达到最大重试次数 {}，停止重连", retryConfig.getMaxRetries());
            ctx.fireEvent(AgentEvent.LOCAL_GOAWAY);
            return;
        }

        long delay = calculateDelay(ctx, retryConfig);
        logger.warn("连接失败，第 {} 次重连将在 {} 秒后执行...", currentRetry, delay);

        ctx.getControlWorkerGroup().schedule(() -> {
            logger.info("开始执行第 {} 次重连", currentRetry);
            ctx.fireEvent(AgentEvent.RETRY);
        }, delay, TimeUnit.SECONDS);
    }

    /**
     * 指数退避 + 抖动
     */
    private long calculateDelay(AgentContext ctx, RetryConfig retryConfig) {
        int retries = ctx.getRetryCount().get();

        if (retries <= 1) {
            return retryConfig.getInitialDelay();
        }
        // 指数退避
        long delay = Math.min(1L << (retries - 1), retryConfig.getMaxDelay());
        // ±30% 随机抖动
        long jitter = (long) (delay * 0.3 * (Math.random() * 2 - 1));

        return Math.min(delay + jitter, retryConfig.getMaxDelay());
    }
}