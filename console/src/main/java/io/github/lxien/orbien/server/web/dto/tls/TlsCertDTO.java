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

package io.github.lxien.orbien.server.web.dto.tls;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;

@Data
public class TlsCertDTO implements Serializable {
    private String id;
    private String org;
    private String issuer;
    private List<String> sanDomains;
    private Integer status;
    private Integer source;
    private Long boundDomainCount;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate notBefore;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate notAfter;
    private Boolean autoRenew;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private java.time.LocalDateTime lastRenewAt;
}
