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

package io.github.lxien.orbien.server.web.proxy.repository;

import io.github.lxien.orbien.server.service.repository.AgentQueryRepository;
import io.github.lxien.orbien.server.statemachine.agent.AgentInfo;
import io.github.lxien.orbien.server.web.proxy.converter.AgentModelConvert;
import io.github.lxien.orbien.server.web.entity.AgentDO;
import io.github.lxien.orbien.server.web.repository.AgentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class AgentQueryRepositoryImpl implements AgentQueryRepository {
    @Autowired
    private AgentRepository agentRepository;
    @Autowired
    private AgentModelConvert agentModelConvert;

    @Override
    public AgentInfo findById(String agentId) {
        Optional<AgentDO> agentDO = agentRepository.findById(agentId);
        return agentDO.map(aDo -> agentModelConvert.toAgentInfo(aDo)).orElse(null);
    }
}
