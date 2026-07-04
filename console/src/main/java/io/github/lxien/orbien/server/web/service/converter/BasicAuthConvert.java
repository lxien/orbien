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

import io.github.lxien.orbien.core.domain.BasicAuthConfig;
import io.github.lxien.orbien.core.domain.HttpUser;
import io.github.lxien.orbien.server.web.dto.basicauth.BasicAuthDetailDTO;
import io.github.lxien.orbien.server.web.dto.basicauth.BasicUserDTO;
import io.github.lxien.orbien.server.web.entity.BasicAuthDO;
import io.github.lxien.orbien.server.web.entity.BasicUserDO;
import io.github.lxien.orbien.server.web.param.basicauth.httpuser.HttpUserAddParam;
import io.github.lxien.orbien.server.web.param.basicauth.httpuser.HttpUserUpdateParam;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;
import java.util.Set;

@Mapper(componentModel = "spring")
public interface BasicAuthConvert {
    @Mapping(expression = "java(toUserDTOList(basicUserDOS))", target = "users")
    BasicAuthDetailDTO toDetailDTO(BasicAuthDO basicAuthDO, List<BasicUserDO> basicUserDOS);

    BasicUserDTO toUserDTO(BasicUserDO basicUserDO);

    List<BasicUserDTO> toUserDTOList(List<BasicUserDO> basicUserDOS);

    BasicUserDO toUserDO(HttpUserAddParam param);

    void updateUserDO(@MappingTarget BasicUserDO basicUserDO, HttpUserUpdateParam param);


    @Mapping(expression = "java(toUserDTOList(httpUsers))", target = "users")
    BasicAuthDetailDTO toDetailDTO(BasicAuthConfig basicAuthConfig, Set<HttpUser> httpUsers);

    List<BasicUserDTO> toUserDTOList(Set<HttpUser> httpUsers);
}


