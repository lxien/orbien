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

import com.xiaoniucode.etp.core.enums.DomainType;
import com.xiaoniucode.etp.server.web.entity.converter.DomainTypeConverter;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 代理域名实体类
 */
@Data
@Entity
@Table(name = "http_proxy_domain")
@NoArgsConstructor
public class ProxyDomainDO {
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
     * 原始域名 子域名前缀/自定义域名(完整域名)
     */
    @Column(nullable = false)
    private String domain;

    @Column(name = "root_domain")
    private String rootDomain;

    @Column(name = "full_domain", nullable = false, unique = true)
    private String fullDomain;
    /**
     * 域名类型
     */
    @Convert(converter = DomainTypeConverter.class)
    @Column(name = "domain_type")
    private DomainType domainType;

    public ProxyDomainDO(String proxyId, String domain, String rootDomain, DomainType domainType) {
        this.proxyId = proxyId;
        this.domain = domain;
        this.rootDomain = rootDomain;
        this.domainType = domainType;
        this.fullDomain = getFullDomain();
    }

    public String getFullDomain() {
        if (domainType == DomainType.CUSTOM_DOMAIN) {
            return domain;
        }
        return domain + "." + rootDomain;
    }
}