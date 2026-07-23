package io.github.lxien.orbien.server.inspector.replay;

import io.github.lxien.orbien.server.inspector.HttpCaptureRecord;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 从捕获记录重建 HTTP/1.1 请求报文
 */
public final class HttpRequestRebuilder {
    public static final Set<String> ALLOWED_METHODS = Set.of(
            "GET", "HEAD", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"
    );

    private static final Set<String> HOP_BY_HOP = Set.of(
            "connection", "keep-alive", "proxy-connection", "transfer-encoding",
            "te", "trailer", "upgrade", "content-length"
    );

    private static final Set<String> X_FORWARDED = Set.of(
            "x-forwarded-for", "x-forwarded-proto", "x-forwarded-host",
            "x-forwarded-port", "forwarded"
    );

    public static final String REPLAY_ID_HEADER = "Orbien-Replay-Original-Request-ID";

    private HttpRequestRebuilder() {
    }

    public static ByteBuf build(HttpCaptureRecord record,
                                ReplayOverrides overrides,
                                String clientIp,
                                String scheme,
                                String sourceRecordId) {
        return build(record, overrides, clientIp, scheme, sourceRecordId, ByteBufAllocator.DEFAULT);
    }

    public static ByteBuf build(HttpCaptureRecord record,
                                ReplayOverrides overrides,
                                String clientIp,
                                String scheme,
                                String sourceRecordId,
                                ByteBufAllocator allocator) {
        if (record == null) {
            throw ReplayException.badRequest("捕获记录为空");
        }
        WorkingRequest working = fromRecord(record);
        applyOverrides(working, overrides);
        normalizeMethod(working);
        applySystemMutations(working, clientIp, scheme, sourceRecordId);
        return encode(working, allocator != null ? allocator : ByteBufAllocator.DEFAULT);
    }

    private static WorkingRequest fromRecord(HttpCaptureRecord record) {
        WorkingRequest working = new WorkingRequest();
        working.method = record.getMethod();
        working.path = StringUtils.hasText(record.getPath()) ? record.getPath() : "/";
        working.headers = copyHeaders(record.getRequestHeaders());
        working.body = record.getRequestBodyPreview() != null ? record.getRequestBodyPreview() : "";
        if (!StringUtils.hasText(headerValue(working.headers, "Host")) && StringUtils.hasText(record.getHost())) {
            working.headers.put("Host", record.getHost());
        }
        return working;
    }

    private static void applyOverrides(WorkingRequest working, ReplayOverrides overrides) {
        if (overrides == null || overrides.isEmpty()) {
            return;
        }
        if (overrides.getMethod() != null) {
            working.method = overrides.getMethod().trim();
        }
        if (overrides.getPath() != null) {
            String path = overrides.getPath().trim();
            working.path = path.isEmpty() ? "/" : path;
        }
        if (overrides.getBody() != null) {
            working.body = overrides.getBody();
        }
        if (overrides.getHeaders() != null) {
            String forbidden = ReplayForbiddenHeaders.findForbidden(overrides.getHeaders());
            if (forbidden != null) {
                throw ReplayException.badRequest("不允许修改 Header: " + forbidden);
            }
            String host = headerValue(working.headers, "Host");
            Map<String, String> merged = new LinkedHashMap<>();
            for (Map.Entry<String, String> entry : overrides.getHeaders().entrySet()) {
                if (entry.getKey() == null || entry.getKey().isBlank()) {
                    continue;
                }
                String name = entry.getKey().trim();
                // 同名（大小写不敏感）去重，保留最后一次
                removeHeaderIgnoreCase(merged, name);
                merged.put(name, entry.getValue() != null ? entry.getValue() : "");
            }
            // Host 始终锁定为捕获原值，不可被 overrides 覆盖
            if (host != null) {
                removeHeaderIgnoreCase(merged, "Host");
                merged.put("Host", host);
            }
            working.headers = merged;
        }
    }

    private static void normalizeMethod(WorkingRequest working) {
        if (!StringUtils.hasText(working.method)) {
            throw ReplayException.badRequest("HTTP 方法未知，无法重放");
        }
        working.method = working.method.trim().toUpperCase(Locale.ROOT);
        if (!ALLOWED_METHODS.contains(working.method)) {
            throw ReplayException.badRequest("不支持的 HTTP 方法: " + working.method);
        }
        if ("CONNECT".equals(working.method)) {
            throw ReplayException.badRequest("CONNECT 请求无法重放");
        }
    }

    private static void applySystemMutations(WorkingRequest working,
                                             String clientIp,
                                             String scheme,
                                             String sourceRecordId) {
        removeHeaders(working.headers, HOP_BY_HOP);
        removeHeaders(working.headers, X_FORWARDED);
        removeHeaderIgnoreCase(working.headers, REPLAY_ID_HEADER);

        byte[] bodyBytes = working.body != null
                ? working.body.getBytes(StandardCharsets.UTF_8)
                : new byte[0];
        working.bodyBytes = bodyBytes;

        working.headers.put("Connection", "close");
        working.headers.put("Content-Length", String.valueOf(bodyBytes.length));

        String ip = StringUtils.hasText(clientIp) ? clientIp : "127.0.0.1";
        working.headers.put("X-Forwarded-For", ip);
        working.headers.put("X-Forwarded-Proto", StringUtils.hasText(scheme) ? scheme : "http");
        String host = headerValue(working.headers, "Host");
        if (StringUtils.hasText(host)) {
            working.headers.put("X-Forwarded-Host", host);
        }
        if (StringUtils.hasText(sourceRecordId)) {
            working.headers.put(REPLAY_ID_HEADER, sourceRecordId);
        }
    }

    private static ByteBuf encode(WorkingRequest working, ByteBufAllocator allocator) {
        StringBuilder sb = new StringBuilder(256 + working.headers.size() * 48);
        sb.append(working.method).append(' ').append(working.path).append(" HTTP/1.1\r\n");
        for (Map.Entry<String, String> entry : working.headers.entrySet()) {
            sb.append(entry.getKey()).append(": ").append(entry.getValue()).append("\r\n");
        }
        sb.append("\r\n");
        byte[] headerBytes = sb.toString().getBytes(StandardCharsets.UTF_8);
        byte[] bodyBytes = working.bodyBytes != null ? working.bodyBytes : new byte[0];
        ByteBuf buf = allocator.buffer(headerBytes.length + bodyBytes.length);
        buf.writeBytes(headerBytes);
        if (bodyBytes.length > 0) {
            buf.writeBytes(bodyBytes);
        }
        return buf;
    }

    private static Map<String, String> copyHeaders(Map<String, String> source) {
        Map<String, String> copy = new LinkedHashMap<>();
        if (source == null) {
            return copy;
        }
        for (Map.Entry<String, String> entry : source.entrySet()) {
            if (entry.getKey() == null) {
                continue;
            }
            copy.put(entry.getKey(), entry.getValue() != null ? entry.getValue() : "");
        }
        return copy;
    }

    private static void removeHeaders(Map<String, String> headers, Set<String> names) {
        headers.entrySet().removeIf(e -> e.getKey() != null
                && names.contains(e.getKey().toLowerCase(Locale.ROOT)));
    }

    private static void removeHeaderIgnoreCase(Map<String, String> headers, String name) {
        headers.entrySet().removeIf(e -> e.getKey() != null && e.getKey().equalsIgnoreCase(name));
    }

    private static String headerValue(Map<String, String> headers, String name) {
        if (headers == null || name == null) {
            return null;
        }
        String direct = headers.get(name);
        if (direct != null) {
            return direct;
        }
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            if (name.equalsIgnoreCase(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }

    /**
     * 仅用于单测断言，避免依赖 Netty buffer
     */
    static String buildRequestText(HttpCaptureRecord record,
                                   ReplayOverrides overrides,
                                   String clientIp,
                                   String scheme,
                                   String sourceRecordId) {
        ByteBuf buf = build(record, overrides, clientIp, scheme, sourceRecordId, ByteBufAllocator.DEFAULT);
        try {
            return buf.toString(StandardCharsets.UTF_8);
        } finally {
            buf.release();
        }
    }

    private static final class WorkingRequest {
        private String method;
        private String path;
        private Map<String, String> headers;
        private String body;
        private byte[] bodyBytes;
    }
}
