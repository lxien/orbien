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

import com.xiaoniucode.etp.core.domain.ProxyConfig;
import com.xiaoniucode.etp.core.domain.ProxyConfigExt;
import com.xiaoniucode.etp.core.domain.DomainInfo;
import com.xiaoniucode.etp.server.service.repository.ProxyQueryRepository;
import com.xiaoniucode.etp.server.web.proxy.repository.assembler.ProxyConfigAssembler;
import com.xiaoniucode.etp.server.web.dto.proxy.ProxyDetailQueryResult;
import com.xiaoniucode.etp.server.web.entity.*;
import com.xiaoniucode.etp.server.web.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

@Repository
public class ProxyQueryRepositoryImpl implements ProxyQueryRepository {
    @Autowired
    private ProxyRepository proxyRepository;
    @Autowired
    private ProxyConfigAssembler proxyConfigAssembler;
    @Autowired
    private ProxyTargetRepository proxyTargetRepository;
    @Autowired
    private ProxyDomainRepository proxyDomainRepository;
    @Autowired
    private BasicUserRepository basicUserRepository;
    @Autowired
    private AccessControlRuleRepository accessControlRuleRepository;

    @Override
    public ProxyConfigExt findById(String proxyId) {
        ProxyDetailQueryResult result = proxyRepository.findDetailByProxyId(proxyId);
        return assembleProxyConfig(result);
    }

    private ProxyConfigExt assembleProxyConfig(ProxyDetailQueryResult result) {
        if (result == null) {
            return null;
        }
        ProxyConfig config = proxyConfigAssembler.assembleBase(result);
        if (config == null) {
            return null;
        }
        String proxyId = config.getProxyId();
        //访问控制
        List<AccessControlRuleDO> accessControlRuleDOS = accessControlRuleRepository.findByProxyId(proxyId);
        proxyConfigAssembler.assembleAccessControlRules(config, accessControlRuleDOS);
        //服务
        List<ProxyTargetDO> targets = proxyTargetRepository.findByProxyId(config.getProxyId());
        proxyConfigAssembler.assembleTargets(config, targets);
        if (config.getProtocol().isHttp()) {
            //域名
            List<ProxyDomainDO> domainDOs = proxyDomainRepository.findByProxyId(config.getProxyId());
            proxyConfigAssembler.assembleDomains(config, domainDOs);
            //鉴权认证
            if (result.getBasicAuthDO() != null) {
                List<BasicUserDO> basicUsers = basicUserRepository.findByProxyId(proxyId);
                proxyConfigAssembler.assembleBasicAuthUsers(config, basicUsers);
            }
        }
        return new ProxyConfigExt();
    }

    @Override
    public List<Integer> findAllListenPorts() {
        return proxyRepository.findAllListenPorts();
    }

    @Override
    public ProxyConfigExt findByAgentAndName(String agentId, String proxyName) {
        ProxyDetailQueryResult result = proxyRepository.findDetailByAgentIdAndProxyName(agentId, proxyName);
        return assembleProxyConfig(result);
    }

    @Override
    public ProxyConfigExt findByListenPort(int listenPort) {
        ProxyDetailQueryResult result = proxyRepository.findDetailByListenPort(listenPort);
        return assembleProxyConfig(result);
    }


    @Override
    public List<ProxyConfigExt> findByAgentId(String agentId) {
        List<ProxyDO> list = proxyRepository.findByAgentId(agentId);
        if (CollectionUtils.isEmpty(list)) {
            return List.of();
        }
        List<String> proxyIds = list.stream().map(ProxyDO::getId).toList();
        List<ProxyDomainDO> proxyDomainDOs = proxyDomainRepository.findByProxyIdIn(proxyIds);
        Map<String, List<ProxyDomainDO>> domainMap = proxyDomainDOs.stream()
                .collect(Collectors.groupingBy(ProxyDomainDO::getProxyId));
        return proxyConfigAssembler.assembleList(list).stream()
                .map(config -> {
                    List<ProxyDomainDO> domains = domainMap.getOrDefault(config.getProxyId(), List.of());
                    Set<DomainInfo> domainInfos = domains.stream()
                            .map(domainDO -> new DomainInfo(domainDO.getBaseDomain(), domainDO.getDomain(), domainDO.getDomainType()))
                            .collect(Collectors.toSet());
                    return new ProxyConfigExt(config, domainInfos);
                })
                .toList();
    }
}
