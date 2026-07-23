package io.github.lxien.orbien.server.inspector;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class HttpCaptureRecordSummary {
    private final String id;
    private final String proxyId;
    private final int streamId;
    private final Instant startedAt;
    private final long durationMs;
    private final String method;
    private final String path;
    private final String host;
    private final String scheme;
    private final int status;
    private final String statusText;
    @Builder.Default
    private final boolean replay = false;
    private final String sourceRecordId;
}
