/*
 *    Copyright 2026 xiaoniucode
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
package com.xiaoniucode.etp.server.web.entity;

import jakarta.persistence.*;
import lombok.Data;

/**
 * 代理目标实体类
 */
@Data
@Entity
@Table(name = "proxy_target", indexes = {
        @Index(name = "idx_proxy_target_proxy_id", columnList = "proxy_id"),
})
public class ProxyTargetDO {
    /**
     * 主键ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    /**
     * 代理ID
     */
    @Column(name = "proxy_id", nullable = false)
    private String proxyId;
    /**
     * 目标主机
     */
    @Column(nullable = false, length = 100)
    private String host;
    /**
     * 目标端口
     */
    @Column(nullable = false)
    private Integer port;
    /**
     * 权重
     */
    @Column(nullable = false, columnDefinition = "INT DEFAULT 1")
    private Integer weight = 1;
    /**
     * 目标名称
     */
    @Column(length = 100)
    private String name;
}