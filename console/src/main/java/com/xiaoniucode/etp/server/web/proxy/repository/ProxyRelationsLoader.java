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

import com.xiaoniucode.etp.server.web.entity.*;
import com.xiaoniucode.etp.server.web.proxy.repository.assembler.ProxyRelations;
import com.xiaoniucode.etp.server.web.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 批量加载代理关联表，避免在组装层散落多次单表查询。
 */
@Component
public class ProxyRelationsLoader {
    @Autowired
    private ProxyTargetRepository proxyTargetRepository;
    @Autowired
    private AccessControlRuleRepository accessControlRuleRepository;
    @Autowired
    private ProxyDomainRepository proxyDomainRepository;
    @Autowired
    private BasicUserRepository basicUserRepository;
    @Autowired
    private HealthCheckRepository healthCheckRepository;

    public ProxyRelations loadOne(String proxyId) {
        return new ProxyRelations(
                proxyTargetRepository.findByProxyId(proxyId),
                accessControlRuleRepository.findByProxyId(proxyId),
                proxyDomainRepository.findByProxyId(proxyId),
                basicUserRepository.findByProxyId(proxyId),
                healthCheckRepository.findById(proxyId).orElse(null)
        );
    }

    public Map<String, ProxyRelations> loadMany(List<String> proxyIds) {
        if (CollectionUtils.isEmpty(proxyIds)) {
            return Map.of();
        }
        List<String> ids = proxyIds.stream().distinct().toList();

        Map<String, List<ProxyTargetDO>> targetsMap = groupByProxyId(
                proxyTargetRepository.findByProxyIdIn(ids), ProxyTargetDO::getProxyId);
        Map<String, List<AccessControlRuleDO>> rulesMap = groupByProxyId(
                accessControlRuleRepository.findByProxyIdIn(ids), AccessControlRuleDO::getProxyId);
        Map<String, List<ProxyDomainDO>> domainsMap = groupByProxyId(
                proxyDomainRepository.findByProxyIdIn(ids), ProxyDomainDO::getProxyId);
        Map<String, List<BasicUserDO>> usersMap = groupByProxyId(
                basicUserRepository.findByProxyIdIn(ids), BasicUserDO::getProxyId);
        Map<String, HealthCheckDO> healthCheckMap = healthCheckRepository.findByProxyIdIn(ids).stream()
                .collect(Collectors.toMap(HealthCheckDO::getProxyId, Function.identity()));

        return ids.stream().collect(Collectors.toMap(
                Function.identity(),
                id -> new ProxyRelations(
                        targetsMap.getOrDefault(id, List.of()),
                        rulesMap.getOrDefault(id, List.of()),
                        domainsMap.getOrDefault(id, List.of()),
                        usersMap.getOrDefault(id, List.of()),
                        healthCheckMap.get(id)
                )
        ));
    }

    private <T> Map<String, List<T>> groupByProxyId(List<T> items, Function<T, String> proxyIdExtractor) {
        return items.stream().collect(Collectors.groupingBy(proxyIdExtractor));
    }
}
