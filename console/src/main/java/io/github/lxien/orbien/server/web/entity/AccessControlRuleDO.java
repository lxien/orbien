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
import jakarta.persistence.*;
import lombok.Data;

/**
 * 访问控制规则实体类
 */
@Data
@Entity
@Table(name = "access_control_rule",
        uniqueConstraints = {@UniqueConstraint(
                name = "uk_proxy_id_cidr",
                columnNames = {"proxy_id", "cidr"}
        )},
        indexes = {@Index(name = "idx_access_control_rule_proxy_id", columnList = "proxy_id")}
)
public class AccessControlRuleDO {
    /**
     * 主键ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    /**
     * 访问控制ID
     */
    private String proxyId;
    /**
     * CIDR 地址段
     */
    private String cidr;
    /**
     * 访问控制模式
     */
    @Convert(converter = AccessControlModeConverter.class)
    private AccessControl mode;
}