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

package io.github.lxien.orbien.server.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.lxien.orbien.core.utils.StringUtils;
import io.github.lxien.orbien.server.service.repository.AgentQueryRepository;
import io.github.lxien.orbien.server.statemachine.agent.AgentInfo;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
public class AgentConfigService {
    private final InternalLogger logger = InternalLoggerFactory.getInstance(AgentConfigService.class);
    @Autowired
    private AgentQueryRepository agentQueryRepository;
    private final Cache<String/*agentId*/, AgentInfo> l1Cache = Caffeine.newBuilder()
            .maximumSize(5000)
            .expireAfterWrite(30, TimeUnit.MINUTES)
            .expireAfterAccess(2, TimeUnit.HOURS)
            .recordStats()
            .build();

    public Optional<AgentInfo> findById(String agentId) {
        if (!StringUtils.hasText(agentId)) {
            return Optional.empty();
        }

        AgentInfo agentInfo = l1Cache.get(agentId, id -> {
            logger.debug("从数据库查询客户端信息：{}", id);
            return agentQueryRepository.findById(id);
        });

        return Optional.ofNullable(agentInfo);
    }

    public void evictById(String agentId) {
        if (StringUtils.hasText(agentId)) {
            l1Cache.invalidate(agentId);
        }
    }

    public void evictByIds(Collection<String> agentIds) {
        if (!CollectionUtils.isEmpty(agentIds)) {
            l1Cache.invalidateAll(agentIds);
        }
    }
}
