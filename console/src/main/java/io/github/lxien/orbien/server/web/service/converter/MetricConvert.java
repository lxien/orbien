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
package io.github.lxien.orbien.server.web.service.converter;

import io.github.lxien.orbien.server.web.dto.metrics.ProxyTrafficQueryResult;
import io.github.lxien.orbien.server.web.dto.metrics.TrafficCountDTO;
import io.github.lxien.orbien.server.web.dto.proxy.ProxyListQueryResult;
import io.github.lxien.orbien.server.web.entity.AgentDO;
import io.github.lxien.orbien.server.web.entity.ProxyDO;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface MetricConvert {

    TrafficCountDTO toDTO(ProxyTrafficQueryResult row);

    List<TrafficCountDTO> toDTOList(List<ProxyTrafficQueryResult> rows);

    default void enrichProxyInfo(TrafficCountDTO dto, ProxyListQueryResult proxyResult) {
        if (dto == null || proxyResult == null || proxyResult.getProxyDO() == null) {
            return;
        }
        ProxyDO proxy = proxyResult.getProxyDO();
        dto.setProxyName(proxy.getName());
        if (proxy.getProtocol() != null) {
            dto.setProtocol(proxy.getProtocol().getCode());
        }
        AgentDO agent = proxyResult.getAgentDO();
        if (agent == null) {
            return;
        }
        dto.setAgentId(agent.getId());
        dto.setAgentName(agent.getName());
    }
}
