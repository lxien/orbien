package io.github.lxien.orbien.server.inspector.replay;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ReplayOptions {
    @Builder.Default
    private final boolean captureToBuffer = true;
    /**
     * 等待响应超时毫秒
     */
    private final long timeoutMs;
    private final ReplayOverrides overrides;
}
