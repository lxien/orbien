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
package io.github.lxien.orbien.server.web.param.headerrewrite;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class HeaderRewriteRuleAddParam {
    @NotNull(message = "代理 ID 不能为空")
    private String proxyId;
    @NotNull(message = "direction 不能为空")
    private Integer direction;
    @NotNull(message = "action 不能为空")
    private Integer action;
    @NotBlank(message = "name 不能为空")
    private String name;
    private String value;
}
