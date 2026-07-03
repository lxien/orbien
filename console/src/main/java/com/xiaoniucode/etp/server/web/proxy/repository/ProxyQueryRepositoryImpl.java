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

package com.xiaoniucode.etp.server.web.proxy.repository;

import com.xiaoniucode.etp.core.domain.ProxyConfigExt;
import com.xiaoniucode.etp.core.enums.ProtocolType;
import com.xiaoniucode.etp.server.service.repository.ProxyQueryRepository;
import com.xiaoniucode.etp.server.web.dto.proxy.ProxyDetailQueryResult;
import com.xiaoniucode.etp.server.web.dto.proxy.ProxyListQueryResult;
import com.xiaoniucode.etp.server.web.entity.*;
import com.xiaoniucode.etp.server.web.proxy.repository.assembler.ProxyConfigAssembler;
import com.xiaoniucode.etp.server.web.proxy.repository.assembler.ProxyRelations;
import com.xiaoniucode.etp.server.web.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Repository
public class ProxyQueryRepositoryImpl implements ProxyQueryRepository {
    @Autowired
    private ProxyRepository proxyRepository;
    @Autowired
    private ProxyConfigAssembler proxyConfigAssembler;
    @Autowired
    private ProxyRelationsLoader proxyRelationsLoader;
    @Autowired
    private AccessControlRepository accessControlRepository;
    @Autowired
    private BasicAuthRepository basicAuthRepository;

    @Override
    public ProxyConfigExt findById(String proxyId) {
        ProxyDetailQueryResult detail = proxyRepository.findDetailByProxyId(proxyId);
        if (detail == null) {
            return null;
        }
        ProxyRelations relations = proxyRelationsLoader.loadOne(proxyId);
        return proxyConfigAssembler.assembleExt(detail, relations);
    }

    @Override
    public List<Integer> findAllListenPorts() {
        return proxyRepository.findAllListenPorts();
    }

    @Override
    public ProxyConfigExt findByAgentAndName(String agentId, String proxyName) {
        ProxyDetailQueryResult detail = proxyRepository.findDetailByAgentIdAndProxyName(agentId, proxyName);
        if (detail == null || detail.getProxyDO() == null) {
            return null;
        }
        ProxyRelations relations = proxyRelationsLoader.loadOne(detail.getProxyDO().getId());
        return proxyConfigAssembler.assembleExt(detail, relations);
    }

    @Override
    public ProxyConfigExt findByListenPort(int listenPort) {
        return findByListenPort(listenPort, ProtocolType.TCP);
    }

    @Override
    public ProxyConfigExt findByListenPort(int listenPort, ProtocolType protocolType) {
        ProxyDetailQueryResult detail = proxyRepository.findDetailByListenPortAndProtocol(listenPort, protocolType);
        if (detail == null || detail.getProxyDO() == null) {
            return null;
        }
        ProxyRelations relations = proxyRelationsLoader.loadOne(detail.getProxyDO().getId());
        return proxyConfigAssembler.assembleExt(detail, relations);
    }

    @Override
    public List<Integer> findListenPortsByProtocol(ProtocolType protocolType) {
        return proxyRepository.findListenPortsByProtocol(protocolType);
    }

    @Override
    public List<ProxyConfigExt> findByAgentId(String agentId) {
        List<ProxyDO> proxies = proxyRepository.findByAgentId(agentId);
        if (CollectionUtils.isEmpty(proxies)) {
            return List.of();
        }

        List<String> proxyIds = proxies.stream().map(ProxyDO::getId).toList();
        Map<String, ProxyRelations> relationsMap = proxyRelationsLoader.loadMany(proxyIds);
        Map<String, AccessControlDO> accessControlMap = accessControlRepository.findAllById(proxyIds).stream()
                .collect(Collectors.toMap(AccessControlDO::getProxyId, Function.identity()));
        Map<String, BasicAuthDO> basicAuthMap = basicAuthRepository.findAllById(proxyIds).stream()
                .collect(Collectors.toMap(BasicAuthDO::getProxyId, Function.identity()));
        Map<String, ProxyListQueryResult> proxyWithAgentMap = proxyRepository.findWithAgentByIdIn(proxyIds).stream()
                .collect(Collectors.toMap(item -> item.getProxyDO().getId(), Function.identity()));

        return proxyIds.stream()
                .map(proxyId -> {
                    ProxyListQueryResult item = proxyWithAgentMap.get(proxyId);
                    if (item == null) {
                        return null;
                    }
                    ProxyDetailQueryResult detail = new ProxyDetailQueryResult(
                            item.getAgentDO(),
                            item.getProxyDO(),
                            basicAuthMap.get(proxyId),
                            accessControlMap.get(proxyId)
                    );
                    ProxyRelations relations = relationsMap.getOrDefault(proxyId, ProxyRelations.empty());
                    return proxyConfigAssembler.assembleExt(detail, relations);
                })
                .filter(item -> item != null)
                .toList();
    }
}
