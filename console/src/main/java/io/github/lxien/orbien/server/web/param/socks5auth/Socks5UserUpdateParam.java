package io.github.lxien.orbien.server.web.param.socks5auth;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Socks5UserUpdateParam {
    @NotNull
    private Long id;
    @NotEmpty
    private String proxyId;
    @NotEmpty
    private String username;
    @NotEmpty
    private String password;
}
