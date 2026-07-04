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
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;
/**
 * 登录令牌实体类
 */
@Data
@Entity
@Table(name = "login_token")
public class LoginTokenDO {
    /**
     * 用户ID
     */
    @Column(name = "uid", nullable = false)
    private Integer uid;
    /**
     * 登录令牌
     */
    @Id
    @Column(name = "token", nullable = false)
    private String token;

    /**
     * 用户名
     */
    @Column(name = "username", nullable = false)
    private String username;
    /**
     * 过期时间戳
     */
    @Column(name = "expire_at", nullable = false)
    private Long expireAt;
    /**
     * 创建时间
     */
    @Column(name = "created_at", nullable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;
}
