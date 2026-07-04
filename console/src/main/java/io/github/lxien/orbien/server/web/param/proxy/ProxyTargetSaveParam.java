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
package io.github.lxien.orbien.server.web.param.proxy;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class ProxyTargetSaveParam {
    @NotEmpty(message = "服务主机号不能为空")
    private String host;
    @NotNull(message = "服务端口号不能为空")
    @Min(value = 1, message = "服务端口号必须大于0")
    @Max(value = 65535, message = "服务端口号必须小于等于65535")
    private Integer port;
    private Integer weight;
    @Size(max = 30, message = "服务名称长度不能超过 30")
    private String name;
}
