package com.xiaoniucode.etp.server.web.entity.converter;

import com.xiaoniucode.etp.server.web.enums.ScheduledJobTriggerType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class ScheduledJobTriggerTypeConverter implements AttributeConverter<ScheduledJobTriggerType, Integer> {
    @Override
    public Integer convertToDatabaseColumn(ScheduledJobTriggerType type) {
        return type != null ? type.getCode() : null;
    }

    @Override
    public ScheduledJobTriggerType convertToEntityAttribute(Integer code) {
        return code != null ? ScheduledJobTriggerType.fromCode(code) : null;
    }
}
