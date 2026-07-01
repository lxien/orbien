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

package com.xiaoniucode.etp.client.statemachine.agent.action;

import com.xiaoniucode.etp.client.manager.ProxyManager;
import com.xiaoniucode.etp.client.manager.ProxyManagerHolder;
import com.xiaoniucode.etp.client.statemachine.ContextConstants;
import com.xiaoniucode.etp.client.statemachine.agent.AgentContext;
import com.xiaoniucode.etp.client.statemachine.agent.AgentEvent;
import com.xiaoniucode.etp.client.statemachine.agent.AgentState;
import com.xiaoniucode.etp.core.message.Message;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

/**
 * 处理来自服务端推送的代理配置信息
 */
public class RuntimeConfigSyncAction extends AgentBaseAction {
    private final InternalLogger logger = InternalLoggerFactory.getInstance(RuntimeConfigSyncAction.class);

    @Override
    protected void doExecute(AgentState from, AgentState to, AgentEvent event, AgentContext context) {
        Message.ProxySyncResponse res = context.getAndRemoveAs(ContextConstants.PROXY_SYNC_RESP,
                Message.ProxySyncResponse.class);
        if (res == null) return;
        ProxyManager proxyManager = ProxyManagerHolder.get();
        Message.ProxySyncType syncType = res.getProxySyncType();

        switch (syncType) {
            case FULL: {
                proxyManager.apply(res.getItemsList());
                break;
            }
            case ADD: {
                proxyManager.batchAdd(res.getItemsList());
                break;
            }
            case UPDATE: {
                proxyManager.batchUpdate(res.getItemsList());
                break;
            }
            case DELETE: {
                proxyManager.batchDelete(res.getProxyIdsList());
                break;
            }
        }
    }
}
