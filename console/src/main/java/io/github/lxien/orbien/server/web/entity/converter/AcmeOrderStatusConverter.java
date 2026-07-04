package io.github.lxien.orbien.server.web.entity.converter;

import io.github.lxien.orbien.server.web.enums.AcmeOrderStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class AcmeOrderStatusConverter implements AttributeConverter<AcmeOrderStatus, Integer> {
    @Override
    public Integer convertToDatabaseColumn(AcmeOrderStatus status) {
        return status != null ? status.getCode() : null;
    }

    @Override
    public AcmeOrderStatus convertToEntityAttribute(Integer code) {
        return code != null ? AcmeOrderStatus.fromCode(code) : null;
    }
}
