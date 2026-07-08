package io.github.lxien.orbien.server.web.param.proxy;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Socks5AuthUserParam {
    private Long id;
    private String username;
    private String password;
}
