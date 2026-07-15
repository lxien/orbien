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
 * HTTP Header 改写动作
 */
@Getter
public enum HeaderAction {
    SET(1, "set"),
    ADD(2, "add"),
    REMOVE(3, "remove");

    private final Integer code;
    private final String value;

    HeaderAction(Integer code, String value) {
        this.code = code;
        this.value = value;
    }

    public static HeaderAction fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (HeaderAction action : values()) {
            if (action.code.equals(code)) {
                return action;
            }
        }
        throw new IllegalArgumentException("未知 Header 动作: " + code);
    }

    public static HeaderAction fromValue(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Header 动作不能为空");
        }
        for (HeaderAction action : values()) {
            if (action.value.equalsIgnoreCase(value) || action.name().equalsIgnoreCase(value)) {
                return action;
            }
        }
        throw new IllegalArgumentException("无效的 Header 动作: " + value);
    }
}
