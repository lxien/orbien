package com.xiaoniucode.etp.server.web.entity.converter;

import com.xiaoniucode.etp.server.web.enums.DnsProviderType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class DnsProviderTypeConverter implements AttributeConverter<DnsProviderType, Integer> {
    @Override
    public Integer convertToDatabaseColumn(DnsProviderType type) {
        return type != null ? type.getCode() : null;
    }

    @Override
    public DnsProviderType convertToEntityAttribute(Integer code) {
        return code != null ? DnsProviderType.fromCode(code) : null;
    }
}
