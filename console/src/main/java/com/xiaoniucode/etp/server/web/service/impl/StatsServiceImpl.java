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

package com.xiaoniucode.etp.server.web.service.impl;


import com.xiaoniucode.etp.core.enums.ProtocolType;
import com.xiaoniucode.etp.core.enums.ProxyStatus;
import com.xiaoniucode.etp.server.statemachine.agent.AgentManager;
import com.xiaoniucode.etp.server.web.dto.stats.DashboardSummaryDTO;
import com.xiaoniucode.etp.server.web.dto.stats.ProxyProtocolCountDTO;
import com.xiaoniucode.etp.server.web.repository.AgentRepository;
import com.xiaoniucode.etp.server.web.repository.ProxyRepository;
import com.xiaoniucode.etp.server.web.service.ProxyService;
import com.xiaoniucode.etp.server.web.service.StatsService;
import jakarta.persistence.Tuple;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class StatsServiceImpl implements StatsService {

    @Autowired
    private AgentRepository agentRepository;
    @Autowired
    private ProxyRepository proxyRepository;
    @Autowired
    private ProxyService proxyService;
    @Autowired
    private AgentManager agentManager;

    @Override
    public DashboardSummaryDTO getDashboardSummary() {
        DashboardSummaryDTO ds = proxyRepository.countTotalAndEnabledCount(ProxyStatus.OPEN);
        ds.setTotalAgents(agentRepository.count());
        ds.setOnlineAgents((long) agentManager.getOnlineCount());
        return ds;
    }

    @Override
    public ProxyProtocolCountDTO getProxyProtocolStats() {
        return proxyRepository.countHttpAndTcp(ProtocolType.HTTP,ProtocolType.HTTPS, ProtocolType.TCP);
    }
}
