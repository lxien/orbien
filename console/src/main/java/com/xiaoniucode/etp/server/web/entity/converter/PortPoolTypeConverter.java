/*
 *    Copyright 2026 xiaoniucode
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
package com.xiaoniucode.etp.server.web.entity.converter;

import com.xiaoniucode.etp.server.web.enums.PortPoolType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class PortPoolTypeConverter implements AttributeConverter<PortPoolType, Integer> {

    @Override
    public Integer convertToDatabaseColumn(PortPoolType type) {
        if (type == null) {
            return null;
        }
        return type.getCode();
    }

    @Override
    public PortPoolType convertToEntityAttribute(Integer code) {
        return PortPoolType.fromCode(code);
    }
}
