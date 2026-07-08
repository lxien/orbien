package io.github.lxien.orbien.server.web.dto.proxy;

import io.github.lxien.orbien.server.web.dto.socks5auth.Socks5UserDTO;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class Socks5ProxyDetailDTO implements Serializable {
    private String id;
    private String agentId;
    private String name;
    private Integer remotePort;
    private Integer listenPort;
    private Integer limitTotal;
    private Boolean authEnabled;
    private List<Socks5UserDTO> authUsers = new ArrayList<>();
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
