/*
 *    Copyright 2026 lxien
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package io.github.lxien.orbien.server.web.entity.converter;

import io.github.lxien.orbien.core.enums.HeaderDirection;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class HeaderDirectionConverter implements AttributeConverter<HeaderDirection, Integer> {
    @Override
    public Integer convertToDatabaseColumn(HeaderDirection attribute) {
        return attribute == null ? null : attribute.getCode();
    }

    @Override
    public HeaderDirection convertToEntityAttribute(Integer dbData) {
        return dbData == null ? null : HeaderDirection.fromCode(dbData);
    }
}
