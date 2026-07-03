package com.xiaoniucode.etp.server.web.param.acme;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class AcmeOrderCreateParam {
    @NotEmpty(message = "请填写申请域名")
    private List<String> domains;

    @NotNull(message = "请选择验证方式")
    private Integer validationMode;

    private Long dnsCredentialId;

    private List<Long> bindProxyDomainIds;

    private Boolean autoRenew = false;
}
