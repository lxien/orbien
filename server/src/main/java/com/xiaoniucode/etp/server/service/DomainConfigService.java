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

package com.xiaoniucode.etp.server.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.xiaoniucode.etp.server.service.repository.DomainQueryRepository;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class DomainConfigService {
    private final InternalLogger logger = InternalLoggerFactory.getInstance(DomainConfigService.class);
    @Autowired
    private DomainQueryRepository domainQueryRepository;

    private final Cache<String, List<String>> baseDomainCache = Caffeine.newBuilder()
            .maximumSize(1)
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .expireAfterAccess(30, TimeUnit.MINUTES)
            .recordStats()
            .build();


    private final Cache<String, Boolean> domainExistsCache = Caffeine.newBuilder()
            .maximumSize(1_0000)
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .recordStats()
            .build();

    public List<String> findAllBaseDomains() {
        return baseDomainCache.get("BASE_DOMAINS", key -> {
            logger.debug("从数据库加载基础域名列表");
            return domainQueryRepository.findAllBaseDomains();
        });
    }

    public boolean exists(String fullDomain) {
        if (!StringUtils.hasText(fullDomain)) {
            return false;
        }
        return domainExistsCache.get(fullDomain, key -> {
            logger.debug("从数据库查询域名是否存在: {}", key);
            return domainQueryRepository.existsByFullDomain(key);
        });
    }

    /**
     * 清理基础域名缓存
     */
    public void evictBaseDomains() {
        baseDomainCache.invalidate("BASE_DOMAINS");
        logger.debug("已清理基础域名列表缓存");
    }

    /**
     * 清理单个域名缓存（当域名被释放时调用）
     */
    public void evictDomain(String fullDomain) {
        if (StringUtils.hasText(fullDomain)) {
            domainExistsCache.invalidate(fullDomain);
            logger.debug("已清理域名缓存: {}", fullDomain);
        }
    }

    public void evictAll() {
        baseDomainCache.invalidateAll();
        domainExistsCache.invalidateAll();
        logger.debug("已清理所有域名缓存");
    }
}
