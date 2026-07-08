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


import io.github.lxien.orbien.core.enums.ProtocolType;
import io.github.lxien.orbien.core.enums.ProxyStatus;
import io.github.lxien.orbien.server.statemachine.agent.AgentManager;
import io.github.lxien.orbien.server.web.dto.stats.DashboardSummaryDTO;
import io.github.lxien.orbien.server.web.dto.stats.ProxyProtocolCountDTO;
import io.github.lxien.orbien.server.web.repository.AgentRepository;
import io.github.lxien.orbien.server.web.repository.ProxyRepository;
import io.github.lxien.orbien.server.web.service.ProxyService;
import io.github.lxien.orbien.server.web.service.StatsService;
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
        ProxyProtocolCountDTO stats = proxyRepository.countProxyProtocolStats(
                ProtocolType.HTTP,
                ProtocolType.HTTPS,
                ProtocolType.TCP,
                ProtocolType.UDP,
                ProtocolType.SOCKS5);
        if (stats == null) {
            return new ProxyProtocolCountDTO(0L, 0L, 0L, 0L, 0L);
        }
        return stats;
    }
}
