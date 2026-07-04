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

import io.github.lxien.orbien.common.utils.StringUtils;
import io.github.lxien.orbien.core.notify.EventBus;
import io.github.lxien.orbien.core.notify.EventListener;
import io.github.lxien.orbien.server.config.AppConfig;
import io.github.lxien.orbien.server.config.domain.DashboardConfig;
import io.github.lxien.orbien.server.event.TunnelServerBindEvent;
import io.github.lxien.orbien.server.web.common.exception.SystemException;
import io.github.lxien.orbien.server.web.entity.SysUserDO;
import io.github.lxien.orbien.server.web.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * 将管理面板用户信息同步至数据库，如果已经存在则不同步
 */
@Component
public class DashboardAdminSynchronizer implements EventListener<TunnelServerBindEvent> {

    private static final Logger logger = LoggerFactory.getLogger(DashboardAdminSynchronizer.class);

    @Resource
    private AppConfig appConfig;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private EventBus eventBus;
    /**
     * 默认角色
     */
    private final static String DEFAULT_ROLE = "R_SUPER";

    @PostConstruct
    public void init() {
        eventBus.register(this);
    }

    @Override
    public void onEvent(TunnelServerBindEvent event) {
        try {
            DashboardConfig dashboard = appConfig.getDashboard();
            if (dashboard == null||!dashboard.getEnabled()) {
                return;
            }
            String username = dashboard.getUsername();
            String password = dashboard.getPassword();

            Optional<SysUserDO> existOpt = userRepository.findByUsername(username);
            if (existOpt.isPresent()) {
                logger.debug("管理员用户已存在于数据库中，跳过同步");
                return;
            }
            if (!StringUtils.hasText(username) || !StringUtils.hasText(password)) {
                logger.warn("Dashboard 管理员用户名或密码未配置，管理面板用户同步失败");
                throw new SystemException("Dashboard 管理员用户名或密码未配置，管理面板用户同步失败");
            }
            SysUserDO sysUserDO = new SysUserDO();
            sysUserDO.setUsername(username);
            sysUserDO.setPassword(passwordEncoder.encode(password));
            sysUserDO.setRole(DEFAULT_ROLE);
            userRepository.save(sysUserDO);
            logger.debug("管理员用户 {} 已成功同步至数据库", username);
        } catch (Exception e) {
            logger.error("管理面板用户同步失败", e);
            throw new SystemException("初始化管理员用户失败，请检查配置和数据库连接", e);
        }
    }
}