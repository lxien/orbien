package com.xiaoniucode.etp.server.web.service.scheduled;

import com.xiaoniucode.etp.server.web.dto.scheduled.ScheduledJobParamFieldDTO;
import com.xiaoniucode.etp.server.web.enums.ScheduledJobCode;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ScheduledJobSchemaProvider {

    public List<ScheduledJobParamFieldDTO> schema(ScheduledJobCode jobCode) {
        return switch (jobCode) {
            case METRICS_CLEANUP -> List.of(
                    field("retentionDays", "保留天数", "number", true, 7, 365,
                            "删除早于该天数的流量统计记录")
            );
            case AGENT_CLEANUP -> {
                List<ScheduledJobParamFieldDTO> fields = new ArrayList<>();
                fields.add(field("inactiveDays", "未活跃天数", "number", true, 7, 365,
                        "超过该天数未活跃的客户端将被清理"));
                fields.add(field("onlyOffline", "仅清理离线客户端", "boolean", true, null, null,
                        "开启后跳过当前在线的客户端"));
                fields.add(field("excludeWithProxy", "跳过有代理的客户端", "boolean", true, null, null,
                        "开启后保留仍有关联代理的客户端"));
                yield fields;
            }
            case ACME_RENEW -> List.of(
                    field("renewBeforeDays", "提前续签天数", "number", true, 7, 60,
                            "在证书到期前多少天开始续签"),
                    field("onlyAcmeSource", "仅 ACME 证书", "boolean", true, null, null,
                            "仅处理通过 ACME 申请的证书"),
                    field("respectCertAutoRenew", "遵循证书自动续签开关", "boolean", true, null, null,
                            "仅续签已开启自动续签的证书"),
                    field("maxCertsPerRun", "单次最多处理", "number", true, 1, 100,
                            "单次任务最多续签的证书数量")
            );
        };
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
