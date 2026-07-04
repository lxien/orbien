package io.github.lxien.orbien.server.web.entity.converter;

import io.github.lxien.orbien.server.web.enums.AcmeValidationMode;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class AcmeValidationModeConverter implements AttributeConverter<AcmeValidationMode, Integer> {
    @Override
    public Integer convertToDatabaseColumn(AcmeValidationMode mode) {
        return mode != null ? mode.getCode() : null;
    }

    @Override
    public AcmeValidationMode convertToEntityAttribute(Integer code) {
        return code != null ? AcmeValidationMode.fromCode(code) : null;
    }
}
