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
package io.github.lxien.orbien.server.web.service.converter;

import io.github.lxien.orbien.server.web.common.utils.DesensitizedUtil;
import io.github.lxien.orbien.server.web.dto.accesstoken.AccessTokenDTO;
import io.github.lxien.orbien.server.web.entity.AccessTokenDO;
import io.github.lxien.orbien.server.web.param.accesstoken.AccessTokenCreateParam;
import io.github.lxien.orbien.server.web.param.accesstoken.AccessTokenUpdateParam;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", imports = DesensitizedUtil.class)
public interface AccessTokenConvert {
    @Named("desensitized")
    @Mapping(target = "token", expression = "java(DesensitizedUtil.token(entity.getToken()))")
    AccessTokenDTO toDTO(AccessTokenDO entity);

    @Named("fullToken")
    AccessTokenDTO toDTOWithFullToken(AccessTokenDO entity);

    @IterableMapping(qualifiedByName = "desensitized")
    List<AccessTokenDTO> toDTOList(List<AccessTokenDO> accessTokenDOS);

    AccessTokenDO toDO(AccessTokenCreateParam param);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "token", ignore = true)
    void updateDO(@MappingTarget AccessTokenDO accessTokenDO, AccessTokenUpdateParam request);
}
