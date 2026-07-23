package io.github.lxien.orbien.server.inspector;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.Map;

@Getter
@Builder
public class HttpCaptureRecord {
    private final String id;
    private final String proxyId;
    private final int streamId;
    private final Instant startedAt;
    private final long durationMs;
    private final String clientIp;
    private final String host;
    private final String method;
    private final String path;
    private final String scheme;
    private final int status;
    private final String statusText;
    private final Map<String, String> requestHeaders;
    private final Map<String, String> responseHeaders;
    private final long requestBodySize;
    private final long responseBodySize;
    private final String requestBodyPreview;
    private final String responseBodyPreview;
    private final boolean requestBodyTruncated;
    private final boolean responseBodyTruncated;
    private final String rawRequest;
    private final String rawResponse;
    /**
     * 是否由重放产生
     */
    @Builder.Default
    private final boolean replay = false;
    /**
     * 重放源记录 ID
     */
    private final String sourceRecordId;

    public HttpCaptureRecordSummary toSummary() {
        return HttpCaptureRecordSummary.builder()
                .id(id)
                .proxyId(proxyId)
                .streamId(streamId)
                .startedAt(startedAt)
                .durationMs(durationMs)
                .method(method)
                .path(path)
                .host(host)
                .scheme(scheme)
                .status(status)
                .statusText(statusText)
                .replay(replay)
                .sourceRecordId(sourceRecordId)
                .build();
    }
}
