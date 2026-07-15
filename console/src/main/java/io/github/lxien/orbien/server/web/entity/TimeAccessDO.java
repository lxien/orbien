/*
 *    Copyright 2026 lxien
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package io.github.lxien.orbien.server.web.entity;

import io.github.lxien.orbien.core.enums.AccessControl;
import io.github.lxien.orbien.core.time.TimeAccessSupport;
import io.github.lxien.orbien.server.web.entity.converter.AccessControlModeConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@Table(name = "time_access")
@NoArgsConstructor
public class TimeAccessDO {
    @Id
    @Column(name = "proxy_id", length = 64)
    private String proxyId;

    @Column(name = "enabled", nullable = false)
    private Boolean enabled;

    @Convert(converter = AccessControlModeConverter.class)
    @Column(name = "mode", nullable = false)
    private AccessControl mode;

    @Column(name = "time_enabled", nullable = false)
    private Boolean timeEnabled;

    @Column(name = "timezone", nullable = false, length = 64)
    private String timezone;

    /**
     * 星期位图：bit0=周一 ... bit6=周日
     */
    @Column(name = "days_mask", nullable = false)
    private Integer daysMask;

    public TimeAccessDO(String proxyId) {
        this.proxyId = proxyId;
        this.enabled = false;
        this.mode = AccessControl.ALLOW;
        this.timeEnabled = true;
        this.timezone = TimeAccessSupport.DEFAULT_TIMEZONE;
        this.daysMask = 0;
    }
}
