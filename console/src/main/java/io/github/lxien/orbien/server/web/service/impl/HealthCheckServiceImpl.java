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
package io.github.lxien.orbien.server.web.service.impl;

import io.github.lxien.orbien.core.enums.HealthCheckType;
import io.github.lxien.orbien.server.web.common.exception.BizException;
import io.github.lxien.orbien.server.web.dto.healthcheck.HealthCheckDTO;
import io.github.lxien.orbien.server.web.entity.HealthCheckDO;
import io.github.lxien.orbien.server.web.param.healthcheck.HealthCheckSaveParam;
import io.github.lxien.orbien.server.web.param.healthcheck.HealthCheckStatusUpdateParam;
import io.github.lxien.orbien.server.web.repository.HealthCheckRepository;
import io.github.lxien.orbien.server.web.repository.ProxyRepository;
import io.github.lxien.orbien.server.web.service.HealthCheckService;
import io.github.lxien.orbien.server.web.service.converter.HealthCheckConvert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class HealthCheckServiceImpl implements HealthCheckService {
    @Autowired
    private HealthCheckRepository healthCheckRepository;
    @Autowired
    private ProxyRepository proxyRepository;
    @Autowired
    private HealthCheckConvert healthCheckConvert;

    @Override
    public HealthCheckDTO getByProxyId(String proxyId) {
        HealthCheckDO healthCheckDO = healthCheckRepository.findById(proxyId)
                .orElseThrow(() -> new BizException("健康检查配置不存在"));
        return healthCheckConvert.toDTO(healthCheckDO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void save(HealthCheckSaveParam param) {
        ensureProxyExists(param.getProxyId());
        HealthCheckType type = HealthCheckType.fromCode(param.getType());
        if (type == null) {
            throw new BizException("健康检查类型无效");
        }
        if (type.isHttpCheck() && !StringUtils.hasText(param.getPath())) {
            throw new BizException("HTTP 健康检查路径不能为空");
        }

        HealthCheckDO healthCheckDO = healthCheckRepository.findById(param.getProxyId())
                .orElseThrow(() -> new BizException("健康检查配置不存在"));
        healthCheckConvert.updateDO(healthCheckDO, param);
        healthCheckDO.setPath(type.isHttpCheck() ? param.getPath() : "/");
        healthCheckRepository.save(healthCheckDO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(HealthCheckStatusUpdateParam param) {
        ensureProxyExists(param.getProxyId());
        HealthCheckDO healthCheckDO = healthCheckRepository.findById(param.getProxyId())
                .orElseThrow(() -> new BizException("健康检查配置不存在"));
        healthCheckDO.setEnabled(param.getEnabled());
        healthCheckRepository.save(healthCheckDO);
    }

    private void ensureProxyExists(String proxyId) {
        if (!proxyRepository.existsById(proxyId)) {
            throw new BizException("代理配置不存在");
        }
    }
}
