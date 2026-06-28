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

package com.xiaoniucode.etp.core.enums;

import lombok.Getter;

@Getter
public enum HealthCheckType {
    TCP("TCP", "TCP 连接检查"),
    HTTP("HTTP", "HTTP 请求检查"),
    AUTO("AUTO", "根据协议自动选择");
    private final String code;
    private final String description;

    HealthCheckType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public static HealthCheckType fromCode(String code) {
        if (code == null) {
            return AUTO;
        }
        for (HealthCheckType type : values()) {
            if (type.getCode().equalsIgnoreCase(code)) {
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

    public boolean isAuto() {
        return this == AUTO;
    }
}