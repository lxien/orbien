package io.github.lxien.orbien.server.web.param.binding;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CertRebindParam {
    @NotBlank(message = "证书ID不能为空")
    private String certId;
}
