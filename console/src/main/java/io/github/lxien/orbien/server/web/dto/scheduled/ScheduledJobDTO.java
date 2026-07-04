package io.github.lxien.orbien.server.web.dto.scheduled;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
public class ScheduledJobDTO implements Serializable {
    private String jobCode;
    private String jobName;
    private String description;
    private Boolean enabled;
    private String cronExpression;
    private Map<String, Object> params;
    private List<ScheduledJobParamFieldDTO> paramSchema = new ArrayList<>();
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastRunAt;
    private Integer lastRunStatus;
    private String lastRunStatusLabel;
    private String lastRunMessage;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime nextRunAt;
}
