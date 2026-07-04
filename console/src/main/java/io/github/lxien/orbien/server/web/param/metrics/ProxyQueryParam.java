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

package io.github.lxien.orbien.server.web.param.metrics;

import io.github.lxien.orbien.server.web.enums.MetricQueryType;
import io.github.lxien.orbien.server.web.support.validation.EnumValue;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class ProxyQueryParam {
    @NotEmpty(message = "代理ID不能为空")
    private String proxyId;
    @NotNull(message = "查询类型不能为空")
    @EnumValue(enumClass = MetricQueryType.class)
    private String queryType;
    private LocalDate startDate;
    private LocalDate endDate;
}