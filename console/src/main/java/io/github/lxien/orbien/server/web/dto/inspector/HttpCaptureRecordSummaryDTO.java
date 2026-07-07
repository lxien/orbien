package io.github.lxien.orbien.server.web.dto.inspector;

import lombok.Data;

import java.time.Instant;

@Data
public class HttpCaptureRecordSummaryDTO {
    private String id;
    private String proxyId;
    private int streamId;
    private Instant startedAt;
    private long durationMs;
    private String method;
    private String path;
    private String host;
    private String scheme;
    private int status;
    private String statusText;
}
