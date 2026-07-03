package com.xiaoniucode.etp.server.web.entity.converter;

import com.xiaoniucode.etp.server.web.enums.ScheduledJobRunStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class ScheduledJobRunStatusConverter implements AttributeConverter<ScheduledJobRunStatus, Integer> {
    @Override
    public Integer convertToDatabaseColumn(ScheduledJobRunStatus status) {
        return status != null ? status.getCode() : null;
    }

    @Override
    public ScheduledJobRunStatus convertToEntityAttribute(Integer code) {
        return code != null ? ScheduledJobRunStatus.fromCode(code) : null;
    }
}
