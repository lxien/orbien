/*
 *
 *  *    Copyright 2026 lxien
 *  *
 *  *    Licensed under the Apache License, Version 2.0 (the "License");
 *  *    you may not use this file except in compliance with the License.
 *  *    You may obtain a copy of the License at
 *  *
 *  *        http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  *    Unless required by applicable law or agreed to in writing, software
 *  *    distributed under the License is distributed on an "AS IS" BASIS,
 *  *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  *    See the License for the specific language governing permissions and
 *  *    limitations under the License.
 *
 */

package io.github.lxien.orbien.server.web.service.converter;

import io.github.lxien.orbien.server.web.dto.tls.TlsCertDTO;
import io.github.lxien.orbien.server.web.entity.TlsCertDO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Mapper(componentModel = "spring")
public interface TlsCertConvert {
    @Mapping(target = "status", expression = "java(sslCertificate.getStatus().getCode())")
    @Mapping(target = "source", expression = "java(sslCertificate.getSource() != null ? sslCertificate.getSource().getCode() : 1)")
    @Mapping(target = "sanDomains", source = "sslCertificate.sanDomains", qualifiedByName = "stringToList")
    @Mapping(target = "boundDomainCount", source = "boundDomainCount")
    @Mapping(target = "autoRenew", source = "sslCertificate.autoRenew")
    @Mapping(target = "lastRenewAt", source = "sslCertificate.lastRenewAt")
    TlsCertDTO toDTO(TlsCertDO sslCertificate, Long boundDomainCount);

    default TlsCertDTO toDTO(TlsCertDO sslCertificate) {
        return toDTO(sslCertificate, 0L);
    }

    @Named("stringToList")
    default List<String> stringToList(String value) {
        if (value == null || value.isEmpty()) {
            return Collections.emptyList();
        }
        return Arrays.asList(value.split(","));
    }
}
