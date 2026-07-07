package io.github.lxien.orbien.server.web.dto.inspector;

import lombok.Data;

import java.time.Instant;
import java.util.Map;

@Data
public class HttpCaptureRecordDTO {
    private String id;
    private String proxyId;
    private int streamId;
    private Instant startedAt;
    private long durationMs;
    private String clientIp;
    private String host;
    private String method;
    private String path;
    private String scheme;
    private int status;
    private String statusText;
    private Map<String, String> requestHeaders;
    private Map<String, String> responseHeaders;
    private long requestBodySize;
    private long responseBodySize;
    private String requestBodyPreview;
    private String responseBodyPreview;
    private boolean requestBodyTruncated;
    private boolean responseBodyTruncated;
    private String rawRequest;
    private String rawResponse;
}
