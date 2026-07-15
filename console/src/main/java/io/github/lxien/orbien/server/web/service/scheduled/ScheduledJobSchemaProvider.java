package io.github.lxien.orbien.server.web.service.scheduled;

import io.github.lxien.orbien.server.web.dto.scheduled.ScheduledJobParamFieldDTO;
import io.github.lxien.orbien.server.web.enums.ScheduledJobCode;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ScheduledJobSchemaProvider {

    public List<ScheduledJobParamFieldDTO> schema(ScheduledJobCode jobCode) {
        if (jobCode == ScheduledJobCode.METRICS_CLEANUP) {
            return metricsCleanupSchema();
        }
        if (jobCode == ScheduledJobCode.AGENT_CLEANUP) {
            return agentCleanupSchema();
        }
        if (jobCode == ScheduledJobCode.ACME_RENEW) {
            return acmeRenewSchema();
        }
        throw new IllegalArgumentException("未知计划任务: " + jobCode);
    }

    private List<ScheduledJobParamFieldDTO> metricsCleanupSchema() {
        return List.of(
                field("retentionDays", "保留天数", "number", true, 7, 365,
                        "删除早于该天数的流量统计记录")
        );
    }

    private List<ScheduledJobParamFieldDTO> agentCleanupSchema() {
        return List.of(
                field("inactiveDays", "未活跃天数", "number", true, 7, 365,
                        "超过该天数未活跃的客户端将被清理"),
                field("onlyOffline", "仅清理离线客户端", "boolean", true, null, null,
                        "开启后跳过当前在线的客户端"),
                field("excludeWithProxy", "跳过有代理的客户端", "boolean", true, null, null,
                        "开启后保留仍有关联代理的客户端")
        );
    }

    private List<ScheduledJobParamFieldDTO> acmeRenewSchema() {
        return List.of(
                field("renewBeforeDays", "提前续签天数", "number", true, 7, 60,
                        "在证书到期前多少天开始续签"),
                field("onlyAcmeSource", "仅 ACME 证书", "boolean", true, null, null,
                        "仅处理通过 ACME 申请的证书"),
                field("respectCertAutoRenew", "遵循证书自动续签开关", "boolean", true, null, null,
                        "仅续签已开启自动续签的证书"),
                field("maxCertsPerRun", "单次最多处理", "number", true, 1, 100,
                        "单次任务最多续签的证书数量")
        );
    }

    private ScheduledJobParamFieldDTO field(
            String key,
            String label,
            String type,
            boolean required,
            Integer min,
            Integer max,
            String description) {
        ScheduledJobParamFieldDTO dto = new ScheduledJobParamFieldDTO();
        dto.setKey(key);
        dto.setLabel(label);
        dto.setType(type);
        dto.setRequired(required);
        dto.setMin(min);
        dto.setMax(max);
        dto.setDescription(description);
        return dto;
    }
}
