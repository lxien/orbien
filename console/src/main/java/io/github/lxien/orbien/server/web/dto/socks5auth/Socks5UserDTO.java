package io.github.lxien.orbien.server.web.dto.socks5auth;

import lombok.Data;

@Data
public class Socks5UserDTO {
    private Long id;
    private String username;
    private String password;
}
