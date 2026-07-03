package com.xiaoniucode.etp.server.web.dto.scheduled;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class ScheduledJobLogDTO implements Serializable {
    private Long id;
    private String jobCode;
    private Integer triggerType;
    private String triggerTypeLabel;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startedAt;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime finishedAt;
    private Integer status;
    private String statusLabel;
    private Integer affectedCount;
    private String message;
}
