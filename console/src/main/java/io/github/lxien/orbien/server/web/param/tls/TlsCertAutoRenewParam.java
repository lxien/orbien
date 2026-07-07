package io.github.lxien.orbien.server.web.param.tls;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TlsCertAutoRenewParam {
    @NotNull(message = "自动续签状态不能为空")
    private Boolean autoRenew;
}
