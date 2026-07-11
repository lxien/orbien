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

package io.github.lxien.orbien.server.event;

import io.github.lxien.orbien.core.message.Message;
import io.github.lxien.orbien.server.notify.Event;
import io.github.lxien.orbien.core.domain.DomainInfo;
import lombok.Getter;

import java.util.List;

@Getter
public class ProxyAddEvent extends Event {
    private final String agentId;
    private final String proxyId;
    private final Message.Proxy proxy;
    private List<DomainInfo> domains;
    private Integer listenPort;

    public ProxyAddEvent(String agentId, String proxyId, Message.Proxy proxy, Integer listenPort) {
        this.agentId = agentId;
        this.proxyId = proxyId;
        this.proxy = proxy;
        this.listenPort = listenPort;
    }

    public ProxyAddEvent(String agentId, String proxyId, Message.Proxy proxy, List<DomainInfo> domains) {
        this.agentId = agentId;
        this.proxyId = proxyId;
        this.proxy = proxy;
        this.domains = domains;
    }
}
