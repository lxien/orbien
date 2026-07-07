/*
 *
 *  *    Copyright 2026 lxien
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

package io.github.lxien.orbien.server.web.entity;

import io.github.lxien.orbien.server.web.entity.converter.TlsCertStatusConverter;
import io.github.lxien.orbien.server.web.enums.CertSource;
import io.github.lxien.orbien.server.web.enums.TlsCertStatus;
import io.github.lxien.orbien.server.web.entity.converter.CertSourceConverter;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "ssl_cert", indexes = {
        @Index(name = "idx_ssl_cert_fingerprint", columnList = "fingerprint")
    })
public class TlsCertDO {
    /**
     * 主键ID
     */
    @Id
    private String id;

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
     * 证书来源
     */
    @Column(name = "cert_source")
    @Convert(converter = CertSourceConverter.class)
    private CertSource source;

    /**
     * 证书状态
     */
    @Column(name = "status")
    @Convert(converter = TlsCertStatusConverter.class)
    private TlsCertStatus status;

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

    @Column(name = "auto_renew")
    private Boolean autoRenew = false;

    @Column(name = "last_renew_at")
    private LocalDateTime lastRenewAt;

    @Column(name = "renew_order_id")
    private Long renewOrderId;

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
