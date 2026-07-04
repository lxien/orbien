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
import io.github.lxien.orbien.core.domain.ProxyConfig;
import io.github.lxien.orbien.core.domain.ProxyConfigExt;
import io.github.lxien.orbien.core.enums.ProtocolType;
import io.github.lxien.orbien.server.service.repository.ProxyQueryRepository;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class ProxyConfigService {
    private final InternalLogger logger = InternalLoggerFactory.getInstance(ProxyConfigService.class);
    @Autowired
    private ProxyQueryRepository proxyQueryRepository;

    /**
     * 一级缓存：各种查询条件 -> proxyId
     */
    private final Cache<String/*bizKey*/, String/*proxyId*/> proxyIdCache = Caffeine.newBuilder()
            .maximumSize(50000)
            .expireAfterWrite(30, TimeUnit.SECONDS)
            .build();

    /**
     * 二级缓存：proxyId -> ProxyConfigExt
     */
    private final Cache<String, ProxyConfigExt> configCache = Caffeine.newBuilder()
            .maximumSize(50000)
            .expireAfterWrite(30, TimeUnit.SECONDS)
            .build();

    public ProxyConfigExt findById(String proxyId) {
        return configCache.get(proxyId, key -> {
            ProxyConfigExt config = proxyQueryRepository.findById(key);
            if (config != null) {
                fillIndexCaches(config);
            }
            return config;
        });
    }

    /**
     * 根据 proxyId 清理所有相关缓存
     */
    public void evictByProxyId(String proxyId) {
        if (proxyId == null) {
            return;
        }

        // 从二级缓存中取出配置（用于构建一级缓存的 key）
        ProxyConfigExt ext = configCache.getIfPresent(proxyId);
        // 清理二级缓存
        configCache.invalidate(proxyId);
        // 清理一级缓存中所有相关的 key
        if (ext != null) {
            ProxyConfig config = ext.getProxyConfig();

            String agentNameKey = "agent:" + config.getAgentId() + ":name:" + config.getName();
            proxyIdCache.invalidate(agentNameKey);

            // 端口索引
            if (config.isTcp() && config.getListenPort() != null) {
                proxyIdCache.invalidate(portCacheKey(ProtocolType.TCP, config.getListenPort()));
            }
            if (config.isUdp() && config.getListenPort() != null) {
                proxyIdCache.invalidate(portCacheKey(ProtocolType.UDP, config.getListenPort()));
            }
        }

        logger.debug("已清理代理缓存: proxyId={}", proxyId);
    }

    /**
     * 批量清理缓存
     */
    public void evictByProxyIds(Collection<String> proxyIds) {
        if (CollectionUtils.isEmpty(proxyIds)) {
            return;
        }

        for (String proxyId : proxyIds) {
            evictByProxyId(proxyId);
        }
        logger.debug("已批量清理代理缓存: count={}", proxyIds.size());
    }

    public void evictAll() {
        proxyIdCache.invalidateAll();
        configCache.invalidateAll();
        logger.debug("已清理所有代理缓存");
    }


    private void fillIndexCaches(ProxyConfigExt ext) {
        ProxyConfig config = ext.getProxyConfig();
        String proxyId = config.getProxyId();
        String agentNameKey = "agent:" + config.getAgentId() + ":name:" + config.getName();
        proxyIdCache.put(agentNameKey, proxyId);
        if (config.isTcp() && config.getListenPort() != null) {
            proxyIdCache.put(portCacheKey(ProtocolType.TCP, config.getListenPort()), proxyId);
        }
        if (config.isUdp() && config.getListenPort() != null) {
            proxyIdCache.put(portCacheKey(ProtocolType.UDP, config.getListenPort()), proxyId);
        }
    }

    private String portCacheKey(ProtocolType protocolType, int listenPort) {
        return "port:" + protocolType.name().toLowerCase() + ":" + listenPort;
    }

    public ProxyConfigExt findByAgentAndName(String agentId, String proxyName) {
        String indexKey = "agent:" + agentId + ":name:" + proxyName;

        // 先查一级缓存
        String proxyId = proxyIdCache.getIfPresent(indexKey);
        if (proxyId != null) {
            // 一级缓存命中，走二级缓存
            return findById(proxyId);
        }

        // 一级缓存未命中，查一次数据库
        ProxyConfigExt ext = proxyQueryRepository.findByAgentAndName(agentId, proxyName);
        if (ext == null) {
            return null;
        }

        // 回填两级缓存
        ProxyConfig config = ext.getProxyConfig();
        proxyIdCache.put(indexKey, config.getProxyId());
        configCache.put(config.getProxyId(), ext);

        return ext;
    }

    public ProxyConfigExt findByListenPort(int listenPort) {
        return findByListenPort(listenPort, ProtocolType.TCP);
    }

    public ProxyConfigExt findByListenPort(int listenPort, ProtocolType protocolType) {
        String indexKey = portCacheKey(protocolType, listenPort);

        // 先查一级缓存
        String proxyId = proxyIdCache.getIfPresent(indexKey);
        if (proxyId != null) {
            return findById(proxyId);
        }

        // 一级缓存未命中，查一次数据库
        ProxyConfigExt ext = proxyQueryRepository.findByListenPort(listenPort, protocolType);
        if (ext == null) {
            return null;
        }
        ProxyConfig config = ext.getProxyConfig();
        // 回填两级缓存
        proxyIdCache.put(indexKey, config.getProxyId());
        configCache.put(config.getProxyId(), ext);

        return ext;
    }

    public List<Integer> getAllListenPorts() {
        return proxyQueryRepository.findAllListenPorts();
    }

    public List<Integer> getListenPorts(ProtocolType protocolType) {
        return proxyQueryRepository.findListenPortsByProtocol(protocolType);
    }

    public List<ProxyConfigExt> findByAgentId(String agentId) {
        return proxyQueryRepository.findByAgentId(agentId);
    }
}