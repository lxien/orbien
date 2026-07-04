package io.github.lxien.orbien.server.web.param.scheduled;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;

@Data
public class ScheduledJobUpdateParam {
    @NotBlank(message = "Cron 表达式不能为空")
    private String cronExpression;

    @NotNull(message = "任务参数不能为空")
    private Map<String, Object> params;
}
