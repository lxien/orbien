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

import com.xiaoniucode.etp.server.web.entity.converter.PortPoolTypeConverter;
import com.xiaoniucode.etp.server.web.enums.PortPoolType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@Entity
@Table(name = "port_pool")
public class PortPoolDO {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "port_start", nullable = false)
    private Integer portStart;
    /**
     * 只有范围端口时才有值，如：8000-8100
     */
    @Column(name = "port_end")
    private Integer portEnd;

    @Convert(converter = PortPoolTypeConverter.class)
    @Column(name = "type", nullable = false)
    private PortPoolType type;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "remark")
    private String remark;

    @Transient
    public boolean isRange() {
        return portEnd != null;
    }

    @Transient
    public String getDisplayText() {
        return isRange() ? portStart + "-" + portEnd : String.valueOf(portStart);
    }
}
