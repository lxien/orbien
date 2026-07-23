package io.github.lxien.orbien.server.web.param.inspector;

import lombok.Data;

import java.util.Map;

@Data
public class ReplayOverridesParam {
    private String method;
    private String path;
    private Map<String, String> headers;
    private String body;
}
