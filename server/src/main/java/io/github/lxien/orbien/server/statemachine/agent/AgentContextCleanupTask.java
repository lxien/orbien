/*
 *    Copyright 2026 lxien
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

package io.github.lxien.orbien.server.statemachine.agent;

import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Component
public class AgentContextCleanupTask {
    private final InternalLogger logger = InternalLoggerFactory.getInstance(AgentContextCleanupTask.class);
    @Autowired
    private AgentManager agentManager;

    /**
     * 每分钟扫描一次；标准客户端断连超过 2 分钟未重连则清理全部资源。
     */
    @Scheduled(fixedDelay = 60_000)
    public void cleanupInactiveContexts() {
        LocalDateTime now = LocalDateTime.now();
        int total = 0;
        int cleaned = 0;
        for (AgentContext context : agentManager.getAllAgentContext()) {
            total++;
            if (context.getState() != AgentState.DISCONNECTED) {
                continue;
            }
            if (ChronoUnit.MINUTES.between(context.getLastActiveTime(), now) >= 2) {
                cleaned++;
                logger.debug("客户端 {} 重连窗口超时，触发 Goaway", context.getAgentId());
                context.fireEvent(AgentEvent.LOCAL_GOAWAY);
            }
        }
        if (cleaned > 0) {
            logger.info("AgentContext cleanup: total={}, cleaned={}", total, cleaned);
        } else {
            logger.debug("AgentContext cleanup: total={}, cleaned={}", total, cleaned);
        }
    }
}