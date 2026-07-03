package com.xiaoniucode.etp.server.web.entity.converter;

import com.xiaoniucode.etp.server.web.enums.CertSource;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class CertSourceConverter implements AttributeConverter<CertSource, Integer> {
    @Override
    public Integer convertToDatabaseColumn(CertSource source) {
        return source != null ? source.getCode() : null;
    }

    @Override
    public CertSource convertToEntityAttribute(Integer code) {
        return code != null ? CertSource.fromCode(code) : null;
    }
}
