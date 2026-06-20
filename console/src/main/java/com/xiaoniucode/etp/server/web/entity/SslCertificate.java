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

import com.xiaoniucode.etp.server.web.enums.SslStatus;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "ssl_certificate")
public class SslCertificate {
    /**
     * 主键ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 证书分类
     * 例如：Let's Encrypt
     */
    private String issuer;
    /**
     * 证书品牌，例如：YR2
     */
    private String issuer0;

    /**
     * 主域名
     */
    private String subject;

    /**
     * SAN域名，可能多个
     */
    private String sanDomains;

    private SslStatus status;

    /**
     * 生效时间
     */
    private LocalDateTime notBefore;

    /**
     * 过期时间
     */
    private LocalDateTime notAfter;

    /**
     * 指纹
     */
    private String fingerprint;

    /**
     * 私钥路径
     */
    private String keyPath;

    /**
     * 证书路径
     */
    private String fullChainPath;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
