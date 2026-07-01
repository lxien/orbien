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

package com.xiaoniucode.etp.server.statemachine.agent.action.config;

import com.xiaoniucode.etp.core.message.Message;
import org.springframework.stereotype.Component;

import java.util.List;


@Component
public class ProxyProcessorFactory {
    private final List<ProxyProcessor> strategies;

    public ProxyProcessorFactory(TcpProxyProcessor tcpStrategy, HttpProxyProcessor httpStrategy) {
        this.strategies = List.of(tcpStrategy, httpStrategy);
    }

    public ProxyProcessor getProcessor(Message.ProtocolType protocolType) {
        return strategies.stream()
                .filter(strategy -> strategy.supports(protocolType))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("不支持的协议类型: " + protocolType));
    }
}
