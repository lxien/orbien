package io.github.lxien.orbien.server.web.param.proxy;

import io.github.lxien.orbien.core.enums.DomainType;
import io.github.lxien.orbien.server.web.support.validation.EnumValue;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class HttpProxyUpdateParam {
    @NotEmpty(message = "id 不能为空")
    private String id;
    @NotEmpty(message = "name 不能为空")
    private String name;
    @NotNull(message = "domainType 不能为空")
    @EnumValue(enumClass = DomainType.class)
    private Integer domainType;
    private List<String> customDomains;
    /** 子域名绑定，每行一条：前缀 + 根域名 */
    private List<SubdomainBindingParam> subdomainBindings;
    @NotEmpty(message = "内网主机不能为空")
    private String localHost;
    @NotNull(message = "内网端口不能为空")
    @Min(value = 1, message = "内网端口不能小于1")
    @Max(value = 65535, message = "内网端口不能大于65535")
    private Integer localPort;
    @Min(value = 1, message = "总带宽限制必须大于0")
    private Integer limitTotal;
}
