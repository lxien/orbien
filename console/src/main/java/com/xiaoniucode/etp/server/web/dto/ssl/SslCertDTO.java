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

package com.xiaoniucode.etp.server.web.dto.ssl;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;

/**
 * SSL证书信息DTO
 * 用于返回证书列表和详情信息
 */
@Data
public class SslCertDTO implements Serializable {
    /**
     * 证书ID
     */
    private String id;

    /**
     * 证书组织
     */
    private String org;

    /**
     * 证书颁发者
     */
    private String issuer;

    /**
     * SAN域名列表
     */
    private List<String> sanDomains;

    /**
     * 证书状态（1-有效，2-过期）
     */
    private Integer status;

    /**
     * 证书生效时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate notBefore;

    /**
     * 证书过期时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate notAfter;
}
