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
package io.github.lxien.orbien.server.web.param.timeaccess;

import io.github.lxien.orbien.core.enums.AccessControl;
import io.github.lxien.orbien.server.web.support.validation.EnumValue;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class TimeAccessUpdateParam {
    @NotEmpty(message = "代理 ID 不能为空")
    private String proxyId;
    @NotNull(message = "启用状态不能为空")
    private Boolean enabled;
    @NotNull(message = "访问模式不能为空")
    @EnumValue(enumClass = AccessControl.class, message = "访问模式值无效")
    private Integer mode;
    @NotNull(message = "时间限制开关不能为空")
    private Boolean timeEnabled;
    private String timezone;
    private List<Integer> days;
}
