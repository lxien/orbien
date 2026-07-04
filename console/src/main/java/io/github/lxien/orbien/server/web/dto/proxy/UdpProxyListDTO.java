package io.github.lxien.orbien.server.web.dto.proxy;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

@EqualsAndHashCode(callSuper = true)
@Data
public class UdpProxyListDTO extends ProxyListDTO implements Serializable {
    private Integer listenPort;
}
