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
package io.github.lxien.orbien.server.web.dto.accesscontrol;
import lombok.Getter;
import lombok.Setter;
import java.util.List;
@Getter
@Setter
public class AccessControlDetailDTO {
    private String proxyId;
    private Boolean enabled;
    private Integer mode;
    private List<AccessControlRuleDTO> rules;
}