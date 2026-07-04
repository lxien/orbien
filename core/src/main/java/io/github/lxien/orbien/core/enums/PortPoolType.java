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

package io.github.lxien.orbien.core.enums;

import lombok.Getter;

@Getter
public enum PortPoolType {
    TCP(1, "TCP"),
    UDP(2, "UDP");

    private final Integer code;
    private final String description;

    PortPoolType(Integer code, String description) {
        this.code = code;
        this.description = description;
    }

    public static PortPoolType fromCode(Integer code) {
        for (PortPoolType status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("未知端口类型: " + code);
    }

    public static PortPoolType fromValue(String value) {
        for (PortPoolType status : values()) {
            if (status.name().equalsIgnoreCase(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("无效的端口类型: " + value);
    }
}
