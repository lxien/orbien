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

package io.github.lxien.orbien.server.web.listener;

import io.github.lxien.orbien.core.notify.EventBus;
import io.github.lxien.orbien.core.notify.EventListener;
import io.github.lxien.orbien.server.config.AppConfig;
import io.github.lxien.orbien.server.config.domain.AuthConfig;
import io.github.lxien.orbien.server.config.domain.TokenConfig;
import io.github.lxien.orbien.server.event.TunnelServerBindEvent;
import io.github.lxien.orbien.server.web.common.exception.SystemException;
import io.github.lxien.orbien.server.web.entity.AccessTokenDO;
import io.github.lxien.orbien.server.web.repository.AccessTokenRepository;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 同步静态访问令牌至数据库
 */
@Component
public class StaticTokenSynchronizer implements EventListener<TunnelServerBindEvent> {
    private final Logger logger = LoggerFactory.getLogger(StaticTokenSynchronizer.class);
    @Autowired
    private AccessTokenRepository accessTokenRepository;
    @Resource
    private AppConfig appConfig;
    @Autowired
    private EventBus eventBus;

    @PostConstruct
    public void init() {
        eventBus.register(this);
    }

    @Override
    public void onEvent(TunnelServerBindEvent event) {
        try {
            AuthConfig authConfig = appConfig.getAuthConfig();
            List<TokenConfig> tokens = authConfig.getTokens();
            for (TokenConfig config : tokens) {
                if (accessTokenRepository.existsByNameOrToken(config.getName(), config.getToken())) {
                    logger.debug("静态访问令牌已存在数据库，跳过同步: 名称={}", config.getName());
                    continue;
                }
                logger.info("静态访问令牌同步成功: 名称={}", config.getName());
                AccessTokenDO accessTokenDO = new AccessTokenDO();
                accessTokenDO.setName(config.getName());
                accessTokenDO.setToken(config.getToken());
                accessTokenDO.setRemark("from toml");
                accessTokenRepository.save(accessTokenDO);
            }
        } catch (Exception e) {
            logger.error("静态令牌同步至数据库发生错误", e);
            throw new SystemException("静态访问令牌同步至数据库时发生错误",e);
        }
    }
}
