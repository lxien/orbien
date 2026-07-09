package io.github.lxien.orbien.server.web.dto.transport;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class TransportProtocolConstraints implements Serializable {
    private List<Integer> availableProtocols;
    private Boolean websocketEnabled;
    private Integer websocketPort;
    private Boolean quicEnabled;
    private Integer quicPort;
    private Integer tcpPort;
}
