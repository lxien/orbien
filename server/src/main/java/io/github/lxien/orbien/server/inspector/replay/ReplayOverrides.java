package io.github.lxien.orbien.server.inspector.replay;

import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Getter
@Builder
public class ReplayOverrides {
    private final String method;
    private final String path;
    /**
     * 非 null 时表示编辑后的完整可编辑 Header 快照（不含 forbidden）
     */
    private final Map<String, String> headers;
    /**
     * 非 null 时覆盖 body；空串表示无 body
     */
    private final String body;

    public boolean isEmpty() {
        return method == null && path == null && headers == null && body == null;
    }
}
