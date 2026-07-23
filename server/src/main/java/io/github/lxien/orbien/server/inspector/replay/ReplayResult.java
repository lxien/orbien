package io.github.lxien.orbien.server.inspector.replay;

import io.github.lxien.orbien.server.inspector.HttpCaptureRecord;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ReplayResult {
    private final String sourceRecordId;
    private final String replayRecordId;
    private final boolean modified;
    private final ReplayStatus status;
    private final HttpCaptureRecord record;
}
