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

import com.xiaoniucode.etp.core.enums.HealthCheckType;
import com.xiaoniucode.etp.server.web.entity.converter.HealthCheckConverter;
import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "health_check")
public class HealthCheckDO {
    @Id
    private String proxyId;
    @Convert(converter = HealthCheckConverter.class)
    @Column(name = "type", nullable = false)
    private HealthCheckType type;

    @Column(name = "interval_sec", nullable = false)
    private Integer interval;

    @Column(name = "timeout_sec", nullable = false)
    private Integer timeout;

    @Column(name = "max_failed", nullable = false)
    private Integer maxFailed;

    @Column(name = "path",nullable = false)
    private String path;

    @Column(name = "enabled", nullable = false)
    private Boolean enabled;
}
