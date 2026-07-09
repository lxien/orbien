package io.github.lxien.orbien.server.web.dto.transport;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class TransportTunnelConstraints implements Serializable {
    private Boolean tunnelEditable;
    private Boolean tunnelLocked;
    private String tunnelLockedReason;
    private List<Integer> allowedTunnelTypes;
}
