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
 * 代理配置信息来源类型
 */
@Getter
public enum ProxySourceType {
    MANUAL(0, "后台手动创建"),
    AGENT(1, "客户端上报");

    private final Integer code;
    private final String description;

    ProxySourceType(Integer code, String description) {
        this.code = code;
        this.description = description;
    }

    public static ProxySourceType fromCode(Integer code) {
        for (ProxySourceType tunnelType : values()) {
            if (tunnelType.getCode().equals(code)) {
                return tunnelType;
            }
        }
        return null;
    }
}
