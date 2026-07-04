package io.github.lxien.orbien.server.web.entity.converter;

import io.github.lxien.orbien.server.web.enums.BindStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class BindStatusConverter implements AttributeConverter<BindStatus, Integer> {
    @Override
    public Integer convertToDatabaseColumn(BindStatus status) {
        return status != null ? status.getCode() : null;
    }

    @Override
    public BindStatus convertToEntityAttribute(Integer code) {
        return code != null ? BindStatus.fromCode(code) : null;
    }
}
