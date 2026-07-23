package io.github.lxien.orbien.server.inspector.replay;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 编辑重放时禁止用户修改的 Header（由系统写入或锁定）
 */
public final class ReplayForbiddenHeaders {
    private static final Set<String> FORBIDDEN = Set.of(
            "connection",
            "keep-alive",
            "proxy-connection",
            "transfer-encoding",
            "te",
            "trailer",
            "upgrade",
            "content-length",
            "x-forwarded-for",
            "x-forwarded-proto",
            "x-forwarded-host",
            "x-forwarded-port",
            "forwarded",
            "host"
    );

    private ReplayForbiddenHeaders() {
    }

    public static boolean isForbidden(String name) {
        if (name == null) {
            return true;
        }
        String lower = name.trim().toLowerCase(Locale.ROOT);
        if (lower.isEmpty()) {
            return true;
        }
        if (lower.startsWith("orbien-replay-") || lower.startsWith(":")) {
            return true;
        }
        return FORBIDDEN.contains(lower);
    }

    /**
     * @return 首个非法 header 名；全部合法返回 null
     */
    public static String findForbidden(Map<String, String> headers) {
        if (headers == null || headers.isEmpty()) {
            return null;
        }
        for (String name : headers.keySet()) {
            if (isForbidden(name)) {
                return name;
            }
        }
        return null;
    }
}
