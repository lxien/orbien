/*
 *
 *  *    Copyright 2026 xiaoniucode
 *  *
 *  *    Licensed under the Apache License, Version 2.0 (the "License");
 *  *    you may not use this file except in compliance with the License.
 *  *    You may obtain a copy of the License at
 *  *
 *  *        http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  *    Unless required by applicable law or agreed to in writing, software
 *  *    distributed under the License is distributed on an "AS IS" BASIS,
 *  *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  *    See the License for the specific language governing permissions and
 *  *    limitations under the License.
 *
 */

package com.xiaoniucode.etp.server.statemachine.agent.action;

import com.xiaoniucode.etp.core.message.Message;
import com.xiaoniucode.etp.server.loadbalance.HealthManager;
import com.xiaoniucode.etp.server.statemachine.agent.AgentConstants;
import com.xiaoniucode.etp.server.statemachine.agent.AgentContext;
import com.xiaoniucode.etp.server.statemachine.agent.AgentEvent;
import com.xiaoniucode.etp.server.statemachine.agent.AgentState;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

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

        List<Message.ServiceHealth> items = req.getItemsList();
        for (Message.ServiceHealth item : items) {
            logger.debug("更新代理服务健康：{},{}:{},{}", item.getProxyId(), item.getHost(), item.getPort(), item.getStatus());
            healthManager.updateHealth(item.getProxyId(), item.getHost(), item.getPort(), item.getStatus());
        }
    }
}
