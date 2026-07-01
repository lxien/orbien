/*
 *    Copyright 2026 xiaoniucode
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

package com.xiaoniucode.etp.server.web.param.portpool;

import com.xiaoniucode.etp.server.web.enums.PortPoolType;
import com.xiaoniucode.etp.server.web.support.validation.EnumValue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PortPoolUpdateParam {
    @NotNull(message = "id 不能为空")
    private Long id;

    @NotBlank(message = "端口不能为空")
    private String port;

    @NotNull(message = "协议类型不能为空")
    @EnumValue(enumClass = PortPoolType.class)
    private Integer type;

    @Size(max = 500, message = "备注长度不能超过 500")
    private String remark;
}
