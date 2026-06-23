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

import com.xiaoniucode.etp.server.web.entity.converter.SslStatusConverter;
import com.xiaoniucode.etp.server.web.enums.SslStatus;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "ssl_certificate", indexes = {
        @Index(name = "idx_ssl_certificate_fingerprint", columnList = "fingerprint")
    })
public class SslCertificateDO {
    /**
     * 主键ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 证书颁发者
     */
    @Column(name = "issuer")
    private String issuer;

    /**
     * 证书组织
     */
    @Column(name = "org")
    private String org;

    /**
     * SAN域名，可能多个
     */
    @Column(name = "san_domains")
    private String sanDomains;

    /**
     * 证书状态
     */
    @Column(name = "status")
    @Convert(converter = SslStatusConverter.class)
    private SslStatus status;

    /**
     * 生效时间
     */
    @Column(name = "not_before")
    private LocalDate notBefore;

    /**
     * 过期时间
     */
    @Column(name = "not_after")
    private LocalDate notAfter;

    /**
     * 指纹
     */
    @Column(name = "fingerprint")
    private String fingerprint;

    /**
     * 私钥路径
     */
    @Column(name = "key_path")
    private String keyPath;

    /**
     * 证书路径
     */
    @Column(name = "full_chain_path")
    private String fullChainPath;

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
