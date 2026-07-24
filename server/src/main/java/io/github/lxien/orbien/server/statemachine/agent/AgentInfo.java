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

package io.github.lxien.orbien.server.statemachine.agent;

import io.github.lxien.orbien.core.enums.AgentType;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Data
public class AgentInfo implements Serializable {
    private String agentId;
    private String name;
    private AgentType agentType;
    private String token;
    private String version;
    private String os;
    private String arch;
    private String sourceIp;
    private LocalDateTime createdAt;
    private LocalDateTime lastActiveTime;

    /**
     * 判断设备是否过期
     * @param timeout 超时数值
     * @param unit 时间单位
     * @return true=已过期
     */
    public boolean isExpired(long timeout, ChronoUnit unit) {
        if (timeout <= 0) {
            return false;
        }

        LocalDateTime baseTime = lastActiveTime != null ? lastActiveTime : createdAt;
        if (baseTime == null) {
            return false;
        }

        LocalDateTime expireTime = baseTime.plus(timeout, unit);
        return LocalDateTime.now().isAfter(expireTime);
    }
}
