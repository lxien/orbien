package com.xiaoniucode.etp.server.web.entity.converter;

import com.xiaoniucode.etp.server.web.enums.DnsCredentialStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class DnsCredentialStatusConverter implements AttributeConverter<DnsCredentialStatus, Integer> {
    @Override
    public Integer convertToDatabaseColumn(DnsCredentialStatus status) {
        return status != null ? status.getCode() : null;
    }

    @Override
    public DnsCredentialStatus convertToEntityAttribute(Integer code) {
        return code != null ? DnsCredentialStatus.fromCode(code) : null;
    }
}
