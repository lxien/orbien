/*
 *    Copyright 2026 lxien
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http:
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package io.github.lxien.orbien.server.web.param.loadbalance;


import io.github.lxien.orbien.core.enums.LoadBalanceType;
import io.github.lxien.orbien.server.web.support.validation.EnumValue;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class LoadBalanceParam {
    @NotNull(message = "负载均衡策略不能为空")
    @EnumValue(enumClass = LoadBalanceType.class)
    private Integer strategy;
}