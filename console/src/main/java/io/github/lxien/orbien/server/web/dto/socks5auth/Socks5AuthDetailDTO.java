package io.github.lxien.orbien.server.web.dto.socks5auth;

import lombok.Data;

import java.util.List;

@Data
public class Socks5AuthDetailDTO {
    private String proxyId;
    private Boolean enabled;
    private List<Socks5UserDTO> users;
}
