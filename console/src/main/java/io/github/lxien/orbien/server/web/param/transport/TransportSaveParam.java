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
package io.github.lxien.orbien.server.web.param.transport;

import io.github.lxien.orbien.core.enums.TunnelType;
import io.github.lxien.orbien.core.enums.TransportProtocol;
import io.github.lxien.orbien.server.web.support.validation.EnumValue;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TransportSaveParam {
    @NotNull(message = "dataProtocol 不能为空")
    @EnumValue(enumClass = TransportProtocol.class, message = "传输协议无效")
    private Integer dataProtocol;

    @NotNull(message = "encrypt 不能为空")
    private Boolean encrypt;

    @NotNull(message = "tunnelType 不能为空")
    @EnumValue(enumClass = TunnelType.class)
    private Integer tunnelType;
}
