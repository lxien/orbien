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

package com.xiaoniucode.etp.server.statemachine.agent.action.config;

import com.xiaoniucode.etp.core.domain.ProxyConfig;
import com.xiaoniucode.etp.server.statemachine.agent.AgentInfo;
import org.springframework.stereotype.Component;

@Component
public class HttpsProxyOperationStrategy implements ProxyConfigOperationStrategy {
    @Override
    public ProxyOperationResult create(ProxyConfig config, AgentInfo agentInfo) throws Exception {
        return null;
    }

    @Override
    public ProxyOperationResult update(ProxyConfig newConfig, ProxyConfig oldConfig, AgentInfo agentInfo) throws Exception {
        return null;
    }

    @Override
    public boolean supports(ProxyConfig config) {
        return false;
    }
}
