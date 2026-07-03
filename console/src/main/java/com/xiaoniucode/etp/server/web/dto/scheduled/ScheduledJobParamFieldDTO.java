package com.xiaoniucode.etp.server.web.dto.scheduled;

import lombok.Data;

import java.io.Serializable;

@Data
public class ScheduledJobParamFieldDTO implements Serializable {
    private String key;
    private String label;
    private String type;
    private boolean required;
    private Integer min;
    private Integer max;
    private String description;
}
