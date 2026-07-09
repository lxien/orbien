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

package io.github.lxien.orbien.core.enums;

import lombok.Getter;

@Getter
public enum TunnelType {
    MULTIPLEX(0, "多路复用"),
    DIRECT(1, "独立连接");

    private final Integer code;
    private final String description;

    TunnelType(Integer code, String description) {
        this.code = code;
        this.description = description;
    }

    public static TunnelType fromCode(Integer code) {
        for (TunnelType tunnelType : values()) {
            if (tunnelType.getCode().equals(code)) {
                return tunnelType;
            }
        }
        return null;
    }

    public boolean isMultiplex() {
        return this == MULTIPLEX;
    }

    public boolean isDirect() {
        return this == DIRECT;
    }
}
