package com.xiaoniucode.etp.server.web.param.dns;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;

@Data
public class DnsCredentialSaveParam {
    private Long id;

    @NotBlank(message = "名称不能为空")
    private String name;

    @NotNull(message = "厂商不能为空")
    private Integer provider;

    @NotNull(message = "配置不能为空")
    private Map<String, String> config;
}
