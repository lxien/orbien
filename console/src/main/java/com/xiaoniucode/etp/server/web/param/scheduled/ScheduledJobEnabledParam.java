package com.xiaoniucode.etp.server.web.param.scheduled;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ScheduledJobEnabledParam {
    @NotNull(message = "启用状态不能为空")
    private Boolean enabled;
}
