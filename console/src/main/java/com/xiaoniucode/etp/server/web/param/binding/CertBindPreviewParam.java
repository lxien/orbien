package com.xiaoniucode.etp.server.web.param.binding;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class CertBindPreviewParam {
    @NotBlank(message = "证书ID不能为空")
    private String certId;

    @NotEmpty(message = "请选择域名")
    private List<Long> proxyDomainIds;
}
