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

package com.xiaoniucode.etp.server.service;

import com.xiaoniucode.etp.core.domain.ProxyConfig;
import com.xiaoniucode.etp.core.domain.ProxyConfigExt;
import com.xiaoniucode.etp.server.service.repository.ProxyQueryRepository;
import com.xiaoniucode.etp.core.domain.DomainInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class ProxyConfigService {

    @Autowired
    private ProxyQueryRepository proxyQueryRepository;

    public Optional<ProxyConfig> findById(String proxyId) {
        return proxyQueryRepository.findById(proxyId);
    }

    public Optional<ProxyConfig> findByDomain(String domain) {
        return proxyQueryRepository.findByFullDomain(domain);
    }

    public Optional<ProxyConfig> findByAgentAndName(String agentId, String proxyName) {
        return proxyQueryRepository.findByAgentAndName(agentId, proxyName);
    }

    public Optional<ProxyConfig> findByListenPort(int remotePort) {

        return proxyQueryRepository.findByListenPort(remotePort);
    }

    public boolean exists(String fullDomain) {
        return proxyQueryRepository.existsByFullDomain(fullDomain);
    }

    public Set<DomainInfo> findDomainsByProxyId(String proxyId) {
        return proxyQueryRepository.findDomainsByProxyId(proxyId);
    }

    public List<Integer> getAllListenPorts() {
        return proxyQueryRepository.findAllListenPorts();
    }

    public List<ProxyConfigExt> findByAgentId(String agentId) {
        return proxyQueryRepository.findByAgentId(agentId);
    }
}