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

/**
 * HTTP Header 改写方向
 */
@Getter
public enum HeaderDirection {
    REQUEST(1, "request"),
    RESPONSE(2, "response");

    private final Integer code;
    private final String value;

    HeaderDirection(Integer code, String value) {
        this.code = code;
        this.value = value;
    }

    public static HeaderDirection fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (HeaderDirection direction : values()) {
            if (direction.code.equals(code)) {
                return direction;
            }
        }
        throw new IllegalArgumentException("未知 Header 方向: " + code);
    }

    public static HeaderDirection fromValue(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Header 方向不能为空");
        }
        for (HeaderDirection direction : values()) {
            if (direction.value.equalsIgnoreCase(value) || direction.name().equalsIgnoreCase(value)) {
                return direction;
            }
        }
        throw new IllegalArgumentException("无效的 Header 方向: " + value);
    }

    public boolean isRequest() {
        return this == REQUEST;
    }

    public boolean isResponse() {
        return this == RESPONSE;
    }
}
