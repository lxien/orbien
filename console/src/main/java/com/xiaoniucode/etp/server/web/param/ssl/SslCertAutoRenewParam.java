package com.xiaoniucode.etp.server.web.param.ssl;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SslCertAutoRenewParam {
    @NotNull(message = "自动续签状态不能为空")
    private Boolean autoRenew;
}
