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

import com.xiaoniucode.etp.server.web.entity.converter.BindStatusConverter;
import com.xiaoniucode.etp.server.web.enums.BindStatus;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "cert_domain_binding", indexes = {
        @Index(name = "idx_cert_binding_cert_id", columnList = "cert_id"),
        @Index(name = "idx_cert_binding_domain", columnList = "domain")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_cert_binding_proxy_domain", columnNames = "proxy_domain_id")
})
public class CertDomainBinding {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "proxy_domain_id", nullable = false)
    private Long proxyDomainId;

    @Column(name = "cert_id", nullable = false)
    private String certId;

    @Column(name = "domain", nullable = false)
    private String domain;

    @Convert(converter = BindStatusConverter.class)
    @Column(name = "status", nullable = false)
    private BindStatus status;

    @Column(name = "enabled", nullable = false)
    private Boolean enabled = true;

    @Column(name = "deploy_version")
    private Integer deployVersion = 0;

    @Column(name = "last_deployed_at")
    private LocalDateTime lastDeployedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
