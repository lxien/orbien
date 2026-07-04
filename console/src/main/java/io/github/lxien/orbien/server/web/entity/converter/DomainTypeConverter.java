/*
 *    Copyright 2026 lxien
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http:
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package io.github.lxien.orbien.server.web.entity.converter;
import io.github.lxien.orbien.core.enums.DomainType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
/**
 * 域名类型转换器
 */
@Converter
public class DomainTypeConverter implements AttributeConverter<DomainType, Integer> {
    /**
     * 将域名类型转换为数据库列值
     * 
     * @param domainType 域名类型
     * @return 数据库列值
     */
    @Override
    public Integer convertToDatabaseColumn(DomainType domainType) {
        if (domainType == null) {
            return null;
        }
        return domainType.getCode();
    }
    /**
     * 将数据库列值转换为域名类型
     * 
     * @param status 数据库列值
     * @return 域名类型
     */
    @Override
    public DomainType convertToEntityAttribute(Integer status) {
        if (status == null) {
            return null;
        }
        return DomainType.fromCode(status);
    }
}