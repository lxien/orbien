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
public enum HealthCheckType {
    TCP(0, "TCP", "TCP 连接检查"),
    HTTP(1, "HTTP", "HTTP 请求检查");

    private final Integer code;
    private final String name;
    private final String description;

    HealthCheckType(Integer code, String name, String description) {
        this.code = code;
        this.name = name;
        this.description = description;
    }

    public static HealthCheckType fromCode(Integer code) {
        for (HealthCheckType type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        return null;
    }

    public static HealthCheckType fromName(String name) {
        for (HealthCheckType type : values()) {
            if (type.getName().equalsIgnoreCase(name)) {
                return type;
            }
        }
        return null;
    }


    public boolean isHttpCheck() {
        return this == HTTP;
    }

    public boolean isTcpCheck() {
        return this == TCP;
    }
}