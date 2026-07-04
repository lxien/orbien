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
import io.github.lxien.orbien.common.utils.StringUtils;
import io.github.lxien.orbien.server.service.repository.TokenQueryRepository;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.concurrent.TimeUnit;

@Service
public class TokenConfigService {
    private final InternalLogger logger = InternalLoggerFactory.getInstance(TokenConfigService.class);
    @Autowired
    private TokenQueryRepository tokenQueryRepository;
    private final Cache<String, Boolean> tokenCache = Caffeine.newBuilder()
            .maximumSize(10000)
            .expireAfterWrite(20, TimeUnit.MINUTES)
            .expireAfterAccess(1, TimeUnit.HOURS)
            .recordStats()
            .build();

    public boolean existsByToken(String token) {
        if (!StringUtils.hasText(token)) {
            return false;
        }
        Boolean exists = tokenCache.get(token, t -> {
                    logger.debug("从数据库查询Token信息：{}", token);
                    return tokenQueryRepository.existsByToken(t);
                }
        );
        return Boolean.TRUE.equals(exists);
    }

    public void evictByToken(String token) {
        if (StringUtils.hasText(token)) {
            tokenCache.invalidate(token);
            logger.debug("清理Token缓存：{}", token);
        }
    }

    public void evictByTokens(Collection<String> tokens) {
        if (CollectionUtils.isNotEmpty(tokens)) {
            tokenCache.invalidateAll(tokens);
            logger.debug("清理Token缓存：{}", tokens);
        }
    }
}
