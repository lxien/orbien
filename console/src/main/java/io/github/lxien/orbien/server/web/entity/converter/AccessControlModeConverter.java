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
import io.github.lxien.orbien.core.enums.AccessControl;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
/**
 * 访问控制模式转换器
 */
@Converter
public class AccessControlModeConverter implements AttributeConverter<AccessControl, Integer> {
    /**
     * 将访问控制模式转换为数据库列值
     * 
     * @param accessControlMode 访问控制模式
     * @return 数据库列值
     */
    @Override
    public Integer convertToDatabaseColumn(AccessControl accessControlMode) {
        if (accessControlMode == null) {
            return null;
        }
        return accessControlMode.getCode();
    }
    /**
     * 将数据库列值转换为访问控制模式
     * 
     * @param code 数据库列值
     * @return 访问控制模式
     */
    @Override
    public AccessControl convertToEntityAttribute(Integer code) {
        if (code == null) {
            return null;
        }
        return AccessControl.fromCode(code);
    }
}
