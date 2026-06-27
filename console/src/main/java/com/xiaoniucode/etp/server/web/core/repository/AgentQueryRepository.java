/*
 *    Copyright 2026 xiaoniucode
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

package com.xiaoniucode.etp.server.web.core.repository;

import com.xiaoniucode.etp.server.statemachine.agent.AgentInfo;
import com.xiaoniucode.etp.server.web.core.converter.AgentModelConvert;
import com.xiaoniucode.etp.server.web.entity.AgentDO;
import com.xiaoniucode.etp.server.web.repository.AgentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class AgentQueryRepository implements com.xiaoniucode.etp.server.service.repository.AgentQueryRepository {
    @Autowired
    private AgentRepository agentRepository;
    @Autowired
    private AgentModelConvert agentModelConvert;

    @Override
    public Optional<AgentInfo> findById(String agentId) {
        Optional<AgentDO> agentDO = agentRepository.findById(agentId);
        return agentDO.map(aDo -> agentModelConvert.toAgentInfo(aDo));
    }
}
