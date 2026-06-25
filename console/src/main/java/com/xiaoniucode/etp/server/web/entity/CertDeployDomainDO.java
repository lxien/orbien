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
import lombok.NoArgsConstructor;

/**
 * 代理证书部署关联的域名，一个代理隧道可能有多个域名
 */
@Data
@Entity
@Table(name = "cert_deploy_domain")
@NoArgsConstructor
public class CertDeployDomainDO {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "deploy_id", nullable = false)
    private Long deployId;

    @Column(name = "domain", nullable = false)
    private String domain;

    public CertDeployDomainDO(Long deployId, String domain) {
        this.deployId = deployId;
        this.domain = domain;
    }
}