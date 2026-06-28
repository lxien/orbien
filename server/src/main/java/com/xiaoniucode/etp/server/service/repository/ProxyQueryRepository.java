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

package com.xiaoniucode.etp.server.service.repository;

import com.xiaoniucode.etp.core.domain.ProxyConfig;
import com.xiaoniucode.etp.core.domain.ProxyConfigExt;
import com.xiaoniucode.etp.core.domain.DomainInfo;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface ProxyQueryRepository {
    Optional<ProxyConfig> findById(String proxyId);

    List<Integer> findAllListenPorts();

    Optional<ProxyConfig> findByAgentAndName(String agentId, String proxyName);

    Optional<ProxyConfig> findByListenPort(int listenPort);

    Optional<ProxyConfig> findByFullDomain(String domain);

    List<ProxyConfigExt> findByAgentId(String agentId);

    boolean existsByFullDomain(String fullDomain);

    Set<DomainInfo> findDomainsByProxyId(String proxyId);
}
