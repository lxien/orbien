package io.github.lxien.orbien.server.web.param.proxy;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class Socks5ProxyUpdateParam {
    @NotEmpty(message = "id 不能为空")
    private String id;
    @NotEmpty(message = "name 不能为空")
    private String name;
    @Min(value = 1, message = "远程端口号不能小于1")
    @Max(value = 65535, message = "远程端口号不能大于65535")
    private Integer remotePort;
    @Min(value = 1, message = "总带宽限制必须大于0")
    private Integer limitTotal;
    private Boolean authEnabled;
    private List<Socks5AuthUserParam> authUsers = new ArrayList<>();
}
