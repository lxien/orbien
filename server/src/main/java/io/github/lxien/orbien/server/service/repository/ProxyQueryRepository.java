/*
 *
 *  *    Copyright 2026 lxien
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

package io.github.lxien.orbien.server.service.repository;

import io.github.lxien.orbien.core.domain.ProxyConfigExt;
import io.github.lxien.orbien.core.enums.ProtocolType;

import java.util.List;

public interface ProxyQueryRepository {
    ProxyConfigExt findById(String proxyId);

    List<Integer> findAllListenPorts();

    List<Integer> findListenPortsByProtocol(ProtocolType protocolType);

    ProxyConfigExt findByAgentAndName(String agentId, String proxyName);

    ProxyConfigExt findByListenPort(int listenPort);

    ProxyConfigExt findByListenPort(int listenPort, ProtocolType protocolType);

    List<ProxyConfigExt> findByAgentId(String agentId);

    List<ProxyConfigExt> findAllOpen();
}
