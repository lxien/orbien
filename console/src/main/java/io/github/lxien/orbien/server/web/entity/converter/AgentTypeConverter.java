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
import io.github.lxien.orbien.core.enums.AgentType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
/**
 * 代理类型转换器
 */
@Converter
public class AgentTypeConverter implements AttributeConverter<AgentType, Integer> {
    /**
     * 将代理类型转换为数据库列值
     * 
     * @param clientType 代理类型
     * @return 数据库列值
     */
    @Override
    public Integer convertToDatabaseColumn(AgentType clientType) {
        if (clientType == null) {
            return null;
        }
        return clientType.getCode();
    }
    /**
     * 将数据库列值转换为代理类型
     * 
     * @param status 数据库列值
     * @return 代理类型
     */
    @Override
    public AgentType convertToEntityAttribute(Integer status) {
        if (status == null) {
            return null;
        }
        return AgentType.fromCode(status);
    }
}