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

package io.github.lxien.orbien.server.web.service.support;

import io.github.lxien.orbien.core.message.Message;
import io.github.lxien.orbien.server.loadbalance.HealthManager;
import io.github.lxien.orbien.server.web.dto.proxy.TargetDTO;
import io.github.lxien.orbien.server.web.repository.HealthCheckRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class TargetHealthEnricher {
    @Autowired
    private HealthManager healthManager;
    @Autowired
    private HealthCheckRepository healthCheckRepository;

    public void enrich(String proxyId, List<TargetDTO> targets) {
        if (CollectionUtils.isEmpty(targets) || !isHealthCheckEnabled(proxyId)) {
            return;
        }
        fillHealthStatus(proxyId, targets);
    }

    public void enrichBatch(Map<String, List<TargetDTO>> targetsByProxyId) {
        if (CollectionUtils.isEmpty(targetsByProxyId)) {
            return;
        }
        Set<String> enabledProxyIds = healthCheckRepository
                .findByProxyIdIn(List.copyOf(targetsByProxyId.keySet()))
                .stream()
                .filter(item -> Boolean.TRUE.equals(item.getEnabled()))
                .map(item -> item.getProxyId())
                .collect(Collectors.toSet());
        for (Map.Entry<String, List<TargetDTO>> entry : targetsByProxyId.entrySet()) {
            if (!enabledProxyIds.contains(entry.getKey())) {
                continue;
            }
            fillHealthStatus(entry.getKey(), entry.getValue());
        }
    }

    private void fillHealthStatus(String proxyId, List<TargetDTO> targets) {
        for (TargetDTO target : targets) {
            Message.HealthStatus status = healthManager.getHealthStatus(
                    proxyId, target.getHost(), target.getPort());
            Integer healthCode = healthManager.toTargetHealthCode(status);
            if (healthCode != null) {
                target.setHealthStatus(healthCode);
            }
        }
    }

    private boolean isHealthCheckEnabled(String proxyId) {
        return healthCheckRepository.findById(proxyId)
                .map(item -> Boolean.TRUE.equals(item.getEnabled()))
                .orElse(false);
    }
}
