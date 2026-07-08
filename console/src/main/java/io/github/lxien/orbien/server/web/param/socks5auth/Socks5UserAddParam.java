package io.github.lxien.orbien.server.web.param.socks5auth;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Socks5UserAddParam {
    @NotEmpty
    private String proxyId;
    @NotEmpty
    private String username;
    @NotEmpty
    private String password;
}
