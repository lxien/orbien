package io.github.lxien.orbien.server.web.dto.proxy;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

@EqualsAndHashCode(callSuper = true)
@Data
public class Socks5ProxyListDTO extends ProxyListDTO implements Serializable {
    private Integer listenPort;
    private Boolean authEnabled;
    private Integer authUserCount;
}
