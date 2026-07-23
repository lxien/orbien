package io.github.lxien.orbien.server.web.param.inspector;

import lombok.Data;

@Data
public class ReplayRequestParam {
    /**
     * 默认 true
     */
    private Boolean captureToBuffer;
    /**
     * 默认取服务端配置
     */
    private Integer timeoutSeconds;
    private ReplayOverridesParam overrides;
}
