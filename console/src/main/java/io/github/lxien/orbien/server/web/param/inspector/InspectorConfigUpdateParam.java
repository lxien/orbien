package io.github.lxien.orbien.server.web.param.inspector;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class InspectorConfigUpdateParam {
    @NotBlank
    private String proxyId;
    private boolean inspectorEnabled;
}
