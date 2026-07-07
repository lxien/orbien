package io.github.lxien.orbien.server.web.dto.inspector;

import lombok.Data;

@Data
public class InspectorConfigDTO {
    private String proxyId;
    private boolean inspectorEnabled;
}
