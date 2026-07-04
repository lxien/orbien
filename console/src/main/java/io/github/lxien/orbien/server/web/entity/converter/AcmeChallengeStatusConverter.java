package io.github.lxien.orbien.server.web.entity.converter;

import io.github.lxien.orbien.server.web.enums.AcmeChallengeStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class AcmeChallengeStatusConverter implements AttributeConverter<AcmeChallengeStatus, Integer> {
    @Override
    public Integer convertToDatabaseColumn(AcmeChallengeStatus status) {
        return status != null ? status.getCode() : null;
    }

    @Override
    public AcmeChallengeStatus convertToEntityAttribute(Integer code) {
        return code != null ? AcmeChallengeStatus.fromCode(code) : null;
    }
}
