package io.github.lxien.orbien.server.web.service.scheduled.job;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.lxien.orbien.server.web.entity.AgentDO;
import io.github.lxien.orbien.server.web.enums.ScheduledJobCode;
import io.github.lxien.orbien.server.web.param.agent.AgentBatchDeleteParam;
import io.github.lxien.orbien.server.web.repository.AgentRepository;
import io.github.lxien.orbien.server.web.repository.ProxyRepository;
import io.github.lxien.orbien.server.web.service.AgentService;
import io.github.lxien.orbien.server.statemachine.agent.AgentManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class AgentCleanupJobHandler implements ScheduledJobHandler {

    private final AgentRepository agentRepository;
    private final ProxyRepository proxyRepository;
    private final AgentService agentService;
    private final AgentManager agentManager;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public ScheduledJobCode jobCode() {
        return ScheduledJobCode.AGENT_CLEANUP;
    }

    @Override
    public JobResult execute(String paramsJson) {
        AgentCleanupJobParams params = parseParams(paramsJson);
        LocalDateTime cutoff = LocalDateTime.now().minusDays(params.getInactiveDays());
        List<AgentDO> candidates = agentRepository.findByLastActiveTimeBefore(cutoff);
        if (CollectionUtils.isEmpty(candidates)) {
            return JobResult.success("没有需要清理的客户端");
        }

        List<String> deleteIds = new ArrayList<>();
        for (AgentDO agent : candidates) {
            if (params.isOnlyOffline() && agentManager.isOnline(agent.getId())) {
                continue;
            }
            if (params.isExcludeWithProxy() && !proxyRepository.findByAgentId(agent.getId()).isEmpty()) {
                continue;
            }
            deleteIds.add(agent.getId());
        }

        if (deleteIds.isEmpty()) {
            return JobResult.success("没有符合清理条件的客户端");
        }

        AgentBatchDeleteParam deleteParam = new AgentBatchDeleteParam();
        deleteParam.setIds(deleteIds);
        agentService.deleteBatch(deleteParam);
        return JobResult.success(deleteIds.size(), "已清理 " + deleteIds.size() + " 个客户端");
    }

    private AgentCleanupJobParams parseParams(String paramsJson) {
        try {
            if (paramsJson == null || paramsJson.isBlank()) {
                return new AgentCleanupJobParams();
            }
            return objectMapper.readValue(paramsJson, AgentCleanupJobParams.class);
        } catch (Exception e) {
            return new AgentCleanupJobParams();
        }
    }
}
