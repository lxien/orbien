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
package io.github.lxien.orbien.server.web.entity;
import io.github.lxien.orbien.core.enums.AccessControl;
import io.github.lxien.orbien.server.web.entity.converter.AccessControlModeConverter;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 访问控制实体类
 */
@Data
@Entity
@Table(name = "access_control")
@NoArgsConstructor
public class AccessControlDO {
    /**
     * 代理ID
     */
    @Id
    private String proxyId;
    /**
     * 是否启用
     */
    private Boolean enabled;
    /**
     * 访问控制模式
     */
    @Convert(converter = AccessControlModeConverter.class)
    private AccessControl mode;

    public AccessControlDO(String proxyId, AccessControl accessControl) {
        this.proxyId=proxyId;
        this.mode=accessControl;
    }
}