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
import io.github.lxien.orbien.core.enums.ProxyStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
/**
 * 代理状态转换器
 */
@Converter
public class ProxyStatusConverter implements AttributeConverter<ProxyStatus, Integer> {
    /**
     * 将代理状态转换为数据库列值
     * 
     * @param proxyStatus 代理状态
     * @return 数据库列值
     */
    @Override
    public Integer convertToDatabaseColumn(ProxyStatus proxyStatus) {
        return proxyStatus.getCode();
    }
    /**
     * 将数据库列值转换为代理状态
     * 
     * @param status 数据库列值
     * @return 代理状态
     */
    @Override
    public ProxyStatus convertToEntityAttribute(Integer status) {
        return ProxyStatus.fromCode(status);
    }
}