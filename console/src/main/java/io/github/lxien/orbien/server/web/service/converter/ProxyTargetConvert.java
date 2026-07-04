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

package io.github.lxien.orbien.server.web.service.converter;

import io.github.lxien.orbien.server.web.dto.proxy.TargetDTO;
import io.github.lxien.orbien.server.web.entity.ProxyTargetDO;
import io.github.lxien.orbien.server.web.param.proxy.ProxyTargetSaveParam;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.Collections;
import java.util.List;

@Mapper(componentModel = "spring")
public interface ProxyTargetConvert {
    @Mapping(target = "proxyId", source = "proxyId")
    ProxyTargetDO toDO(ProxyTargetSaveParam param, String proxyId);

    default List<ProxyTargetDO> toDOList(List<ProxyTargetSaveParam> params, String proxyId) {
        if (params == null) return Collections.emptyList();
        return params.stream()
                .map(param -> toDO(param, proxyId))
                .collect(java.util.stream.Collectors.toList());
    }

    List<TargetDTO> toDTOList(List<ProxyTargetDO> proxyTargetDos);
}

