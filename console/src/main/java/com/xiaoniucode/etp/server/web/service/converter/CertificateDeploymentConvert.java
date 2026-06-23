/*
 *
 *  *    Copyright 2026 xiaoniucode
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

package com.xiaoniucode.etp.server.web.service.converter;

import com.xiaoniucode.etp.server.web.dto.deploy.SslDeployInfoDTO;
import com.xiaoniucode.etp.server.web.entity.CertificateDeploymentDO;
import com.xiaoniucode.etp.server.web.entity.SslCertificateDO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Mapper(componentModel = "spring")
public interface CertificateDeploymentConvert {

    @Mapping(target = "deployId", source = "deploymentDO.id")
    @Mapping(target = "certId", source = "deploymentDO.certId")
    @Mapping(target = "proxyId", source = "deploymentDO.proxyId")
    @Mapping(target = "enabled", source = "deploymentDO.enabled")
    @Mapping(target = "issuer", source = "certificateDO.issuer")
    @Mapping(target = "org", source = "certificateDO.org")
    @Mapping(target = "sanDomains", source = "certificateDO.sanDomains", qualifiedByName = "stringToList")
    @Mapping(target = "notBefore", source = "certificateDO.notBefore")
    @Mapping(target = "notAfter", source = "certificateDO.notAfter")
    @Mapping(target = "keyPem", ignore = true)
    @Mapping(target = "fullChainPem", ignore = true)
    SslDeployInfoDTO toDeployInfoDTO(CertificateDeploymentDO deploymentDO, SslCertificateDO certificateDO);

    @Named("stringToList")
    default List<String> stringToList(String value) {
        if (value == null || value.isEmpty()) {
            return Collections.emptyList();
        }
        return Arrays.asList(value.split(","));
    }
}
