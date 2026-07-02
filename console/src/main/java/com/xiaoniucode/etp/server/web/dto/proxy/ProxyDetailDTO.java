/*
 *    Copyright 2026 xiaoniucode
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

package com.xiaoniucode.etp.server.web.dto.proxy;

import com.xiaoniucode.etp.server.web.dto.bandwidth.BandwidthDTO;
import com.xiaoniucode.etp.server.web.dto.loadbalance.LoadBalanceDTO;
import com.xiaoniucode.etp.server.web.dto.transport.TransportDTO;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ProxyDetailDTO implements Serializable {
    private String id;
    private String agentId;
    private String name;
    private Integer protocol;
    private Integer agentType;
    private Integer status;
    private TransportDTO transport;
    private BandwidthDTO bandwidth;
    private LoadBalanceDTO loadBalance;
    private List<TargetDTO> targets;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
