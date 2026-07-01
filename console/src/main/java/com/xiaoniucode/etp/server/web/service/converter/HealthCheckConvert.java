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
package com.xiaoniucode.etp.server.web.service.converter;

import com.xiaoniucode.etp.core.enums.HealthCheckType;
import com.xiaoniucode.etp.server.web.dto.healthcheck.HealthCheckDTO;
import com.xiaoniucode.etp.server.web.entity.HealthCheckDO;
import com.xiaoniucode.etp.server.web.param.healthcheck.HealthCheckSaveParam;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", imports = {HealthCheckType.class})
public interface HealthCheckConvert {

    @Mapping(expression = "java(healthCheckDO.getType().getCode())", target = "type")
    HealthCheckDTO toDTO(HealthCheckDO healthCheckDO);

    @Mapping(expression = "java(HealthCheckType.fromCode(param.getType()))", target = "type")
    @Mapping(target = "proxyId", ignore = true)
    @Mapping(target = "enabled", ignore = true)
    void updateDO(@MappingTarget HealthCheckDO healthCheckDO, HealthCheckSaveParam param);
}
