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
public enum DeploymentMode {
    CLUSTER(0, "集群"),
    STANDALONE(1, "单机");

    private final Integer code;
    private final String description;

    DeploymentMode(Integer code, String description) {
        this.code = code;
        this.description = description;
    }

    public static DeploymentMode fromCode(Integer code) {
        for (DeploymentMode deploymentMode : values()) {
            if (deploymentMode.getCode().equals(code)) {
                return deploymentMode;
            }
        }
        throw new IllegalArgumentException("不合法输入：" + code);
    }

    public boolean isCluster() {
        return this == CLUSTER;
    }

    public boolean isStandalone() {
        return this == STANDALONE;
    }
}
