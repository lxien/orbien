/*
 *    Copyright 2026 lxien
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http:
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package io.github.lxien.orbien.server.web.service.converter;

import io.github.lxien.orbien.server.statemachine.agent.AgentInfo;
import io.github.lxien.orbien.server.web.dto.agent.AgentDTO;
import io.github.lxien.orbien.server.web.entity.AgentDO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface AgentConvert {
    @Mapping(target = "agentType", expression = "java(agent.getAgentType().getCode())")
    AgentDTO toDTO(AgentDO agent);
    @Mapping(target = "agentType", expression = "java(agent.getAgentType().getCode())")
    @Mapping(target = "id", source = "agentId")
    AgentDTO toDTO(AgentInfo agent);
    List<AgentDTO> toDTOList(List<AgentDO> agents);
}
