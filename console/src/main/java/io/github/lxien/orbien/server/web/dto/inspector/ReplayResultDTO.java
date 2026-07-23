package io.github.lxien.orbien.server.web.dto.inspector;

import lombok.Data;

@Data
public class ReplayResultDTO {
    private String sourceRecordId;
    private String replayRecordId;
    private boolean modified;
    private String status;
    private HttpCaptureRecordDTO record;
}
