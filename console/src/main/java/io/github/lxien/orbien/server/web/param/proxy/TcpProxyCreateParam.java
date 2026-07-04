package io.github.lxien.orbien.server.web.param.proxy;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TcpProxyCreateParam {
    @NotEmpty(message = "agentId 不能为空")
    private String agentId;
    @NotEmpty(message = "name 不能为空")
    private String name;
    @NotEmpty(message = "内网主机不能为空")
    private String localHost;
    @NotNull(message = "内网端口不能为空")
    @Min(value = 1, message = "内网端口不能小于1")
    @Max(value = 65535, message = "内网端口不能大于65535")
    private Integer localPort;
    @Min(value = 1, message = "远程端口号不能小于1")
    @Max(value = 65535, message = "远程端口号不能大于65535")
    private Integer remotePort;
    @Min(value = 1, message = "总带宽限制必须大于0")
    private Integer limitTotal;
}
