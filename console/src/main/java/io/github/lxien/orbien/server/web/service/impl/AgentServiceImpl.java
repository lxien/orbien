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
package io.github.lxien.orbien.server.web.service.impl;

import io.github.lxien.orbien.core.enums.AgentType;
import io.github.lxien.orbien.server.service.AgentConfigService;
import io.github.lxien.orbien.server.statemachine.agent.AgentInfo;
import io.github.lxien.orbien.server.statemachine.agent.AgentManager;
import io.github.lxien.orbien.server.web.common.exception.BizException;
import io.github.lxien.orbien.server.web.common.message.PageQuery;
import io.github.lxien.orbien.server.web.common.message.PageResult;
import io.github.lxien.orbien.server.web.dto.agent.AgentDTO;
import io.github.lxien.orbien.server.web.entity.AgentDO;
import io.github.lxien.orbien.server.web.param.agent.AgentBatchDeleteParam;
import io.github.lxien.orbien.server.web.repository.AgentRepository;
import io.github.lxien.orbien.server.web.service.AgentService;
import io.github.lxien.orbien.server.web.service.converter.AgentConvert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AgentServiceImpl implements AgentService {
    private final Logger logger = LoggerFactory.getLogger(AgentServiceImpl.class);
    @Autowired
    private AgentRepository agentRepository;
    @Autowired
    private AgentConvert agentConvert;
    @Autowired
    private AgentManager agentManager;
    @Autowired
    private AgentConfigService agentConfigService;

    @Override
    public PageResult<AgentDTO> findByPage(PageQuery pageQuery) {
        int currentPage = Math.max(0, pageQuery.getCurrent() - 1);
        Pageable pageable = PageRequest.of(currentPage, pageQuery.getSize());
        Page<AgentDO> agentPage = agentRepository.findAll(pageable);

        if (agentPage.isEmpty()) {
            return PageResult.empty(pageQuery.getCurrent(), pageQuery.getSize());
        }

        List<AgentDO> agents = agentPage.getContent();
        List<AgentDTO> agentDTOList = enrichAgents(agentConvert.toDTOList(agents));
        return PageResult.wrap(agentPage, agentDTOList);
    }

    @Override
    public AgentDTO findById(String agentId) {
        AgentDO agent = agentRepository.findById(agentId).orElseThrow(() -> new BizException("客户端不存在"));
        return enrichAgent(agentConvert.toDTO(agent));
    }

    @Override
    public void kickout(String agentId) {
        if (!agentManager.isOnline(agentId)) {
            throw new BizException("客户端未在线，无法强退");
        }
        logger.info("强制客户端下线：{}", agentId);
        agentManager.kickout(agentId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteBatch(AgentBatchDeleteParam param) {
        List<String> ids = param.getIds();
        if (CollectionUtils.isEmpty(ids)) {
            return;
        }
        ids.forEach(agentId -> {
            if (agentManager.isOnline(agentId)) {
                agentManager.kickout(agentId);
            }
        });
        agentConfigService.evictByIds(ids);
        agentRepository.deleteAllById(ids);
        logger.debug("批量删除客户端成功，数量: {}", ids.size());
    }

    @Override
    public List<AgentDTO> findAll() {
        return enrichAgents(agentConvert.toDTOList(agentRepository.findAll()));
    }

    @Override
    public List<AgentDTO> findForProxySelection(String includeAgentId) {
        List<AgentDTO> standaloneAgents = enrichAgents(
                agentConvert.toDTOList(agentRepository.findByAgentType(AgentType.STANDALONE)));

        Map<String, AgentDTO> result = new LinkedHashMap<>();
        standaloneAgents.forEach(dto -> result.put(dto.getId(), dto));

        if (StringUtils.hasText(includeAgentId)) {
            String agentId = includeAgentId.trim();
            if (!result.containsKey(agentId)) {
                agentRepository.findById(agentId)
                        .map(agentConvert::toDTO)
                        .map(this::enrichAgent)
                        .ifPresent(dto -> result.put(dto.getId(), dto));
            }
        }
        return new ArrayList<>(result.values());
    }

    private List<AgentDTO> enrichAgents(List<AgentDTO> agents) {
        agents.forEach(this::enrichAgent);
        return agents;
    }

    private AgentDTO enrichAgent(AgentDTO dto) {
        String agentId = dto.getId();
        dto.setIsOnline(agentManager.isOnline(agentId));
        agentManager.getAgentContext(agentId).ifPresent(agentContext -> {
            AgentInfo agentInfo = agentContext.getAgentInfo();
            dto.setLastActiveTime(agentInfo.getLastActiveTime());
        });
        return dto;
    }
}
