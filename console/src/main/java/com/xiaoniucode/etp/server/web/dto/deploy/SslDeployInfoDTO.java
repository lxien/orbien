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

package com.xiaoniucode.etp.server.web.dto.deploy;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;

/**
 * SSL部署信息DTO
 * 用于返回代理的SSL证书部署详情
 */
@Data
public class SslDeployInfoDTO implements Serializable {
    /**
     * 部署记录ID
     */
    private Long deployId;

    /**
     * 证书ID
     */
    private String certId;

    /**
     * 代理ID
     */
    private String proxyId;

    /**
     * 证书颁发者
     */
    private String issuer;

    /**
     * 证书组织
     */
    private String org;

    /**
     * SAN域名列表
     */
    private List<String> sanDomains;

    /**
     * 证书生效时间
     */
    private LocalDate notBefore;

    /**
     * 证书过期时间
     */
    private LocalDate notAfter;

    /**
     * 私钥内容（PEM格式）
     */
    private String keyPem;

    /**
     * 完整证书链内容（PEM格式）
     */
    private String fullChainPem;

    /**
     * 是否启用SSL证书
     */
    private Boolean enabled;
}
