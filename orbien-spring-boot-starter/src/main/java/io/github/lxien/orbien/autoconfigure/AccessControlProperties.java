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

package io.github.lxien.orbien.autoconfigure;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
@Data
public class AccessControlProperties  implements Serializable {
    @Setter
    private boolean enabled = false;
    @Setter
    @NestedConfigurationProperty
    private AccessControlMode mode = AccessControlMode.ALLOW;
    private final Set<String> allow = new HashSet<>();
    private final Set<String> deny = new HashSet<>();

    /**
     * IP 访问控制模式
     */
    @Getter
    enum AccessControlMode {
        /**
         * 白名单模式：只允许指定 IP 访问
         */
        ALLOW,
        /**
         * 黑名单模式：拒绝指定 IP 访问，允许其他 IP 访问
         */
        DENY
    }
}
