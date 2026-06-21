/*
 *
 *  *    Copyright 2026 xiaoniucode
 *  *
 *  *    Licensed under the Apache License, Version 2.0 (the "License");
 *  *    you may not use this file except in compliance with the License.
 *  *    You may obtain a copy of the License at
 *  *
 *  *        http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  *    Unless required by applicable law or agreed to in writing, software
 *  *    distributed under the License is distributed on an "AS IS" BASIS,
 *  *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  *    See the License for the specific language governing permissions and
 *  *    limitations under the License.
 *
 */

package com.xiaoniucode.etp.server.web.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 用于给某个HTTPS代理隧道部署SSL域名证书
 */
@Data
@Entity
@Table(name = "certificate_deployment")
public class CertificateDeploymentDO {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    /**
     * 关联的证书ID
     */
    @Column(name = "certificate_id", nullable = false)
    private Long certificateId;
    /**
     * 关联的代理ID
     */
    @Column(name = "proxy_id", nullable = false)
    private Long proxyId;
    /**
     * 绑定的域名
     */
    @Column(name = "domain")
    private String domain;
    /**
     * 是否启用
     */
    @Column(name = "enabled")
    private Boolean enabled = true;

    /**
     * 最后部署时间
     */
    @Column(name = "last_deployed_at", nullable = false)
    private LocalDateTime lastDeployedAt;

    /**
     * 创建时间
     */
    @Column(name = "created_at", nullable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @Column(name = "updated_at", nullable = false)
    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
