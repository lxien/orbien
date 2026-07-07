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

package io.github.lxien.orbien.server.web.enums;

import lombok.Getter;

/**
 * TLS 证书状态
 */
@Getter
public enum TlsCertStatus {
    /** TLS 证书已激活 */
    ACTIVE(1, "已激活"),
    /** TLS 证书已过期 */
    EXPIRED(2, "已过期");

    private final Integer code;
    private final String description;

    TlsCertStatus(Integer code, String description) {
        this.code = code;
        this.description = description;
    }

    public static TlsCertStatus fromCode(Integer code) {
        for (TlsCertStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("未知SSL状态: " + code);
    }

    public static TlsCertStatus fromValue(String value) {
        for (TlsCertStatus status : values()) {
            if (status.name().equalsIgnoreCase(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("无效的SSL状态: " + value);
    }

    public boolean isActive() {
        return this == ACTIVE;
    }

    public boolean isExpired() {
        return this == EXPIRED;
    }
}
