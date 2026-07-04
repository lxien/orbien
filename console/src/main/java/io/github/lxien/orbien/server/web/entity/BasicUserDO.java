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

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@Table(name = "basic_user",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_proxy_id_username", columnNames = {"proxy_id", "username"})
        },
        indexes = {
                @Index(name = "idx_username", columnList = "username")
        }

)
@NoArgsConstructor
public class BasicUserDO {
    /**
     * 主键ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    /**
     * 代理ID
     */
    @Column(name = "proxy_id")
    private String proxyId;
    /**
     * 用户名
     */
    @Column(name = "username")
    private String username;
    /**
     * 密码
     */
    @Column(name = "password")
    private String password;

    public BasicUserDO(String proxyId, String username, String password) {
        this.proxyId = proxyId;
        this.username = username;
        this.password = password;
    }
}
