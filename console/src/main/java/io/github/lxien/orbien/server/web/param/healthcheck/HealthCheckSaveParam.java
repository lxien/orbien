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
package io.github.lxien.orbien.server.web.param.healthcheck;

import io.github.lxien.orbien.core.enums.HealthCheckType;
import io.github.lxien.orbien.server.web.support.validation.EnumValue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class HealthCheckSaveParam {
    @NotNull(message = "代理 ID 不能为空")
    private String proxyId;

    @NotNull(message = "健康检查类型不能为空")
    @EnumValue(enumClass = HealthCheckType.class)
    private Integer type;

    @NotNull(message = "检查间隔不能为空")
    @Min(value = 1, message = "检查间隔必须大于 0")
    private Integer interval;

    @NotNull(message = "超时时间不能为空")
    @Min(value = 1, message = "超时时间必须大于 0")
    private Integer timeout;

    @NotNull(message = "最大失败次数不能为空")
    @Min(value = 1, message = "最大失败次数必须大于 0")
    private Integer maxFailed;

    private String path;
}
