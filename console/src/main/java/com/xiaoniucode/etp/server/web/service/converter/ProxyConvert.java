/*
 *    Copyright 2026 xiaoniucode
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
package com.xiaoniucode.etp.server.web.service.converter;

import com.xiaoniucode.etp.core.domain.ProxyConfig;
import com.xiaoniucode.etp.core.enums.DomainType;
import com.xiaoniucode.etp.core.enums.ProtocolType;
import com.xiaoniucode.etp.server.web.dto.proxy.HttpProxyListDTO;
import com.xiaoniucode.etp.server.web.dto.proxy.TcpProxyDetailDTO;
import com.xiaoniucode.etp.server.web.dto.proxy.TcpProxyListDTO;
import com.xiaoniucode.etp.server.web.dto.proxy.embedded.TunnelDetailDTO;
import com.xiaoniucode.etp.server.web.dto.proxy.embedded.TunnelListDTO;
import com.xiaoniucode.etp.server.web.entity.ProxyDO;
import com.xiaoniucode.etp.server.web.param.proxy.TcpProxyCreateParam;
import com.xiaoniucode.etp.server.web.param.proxy.TcpProxyUpdateParam;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;

@Mapper(componentModel = "spring", imports = {ProtocolType.class, DomainType.class})
public interface ProxyConvert {
    TcpProxyListDTO toTcpListDTO(ProxyDO proxy);

    List<TcpProxyListDTO> toTcpDTOList(List<ProxyDO> proxies);

    @Named("domainTypeToCode")
    static Integer domainTypeToCode(DomainType domainType) {
        return domainType != null ? domainType.getCode() : null;
    }

    @Named("codeToDomainType")
    static DomainType codeToDomainType(Integer code) {
        return DomainType.fromCode(code);
    }

    @Mapping(source = "httpProxyPort", target = "httpProxyPort")
    HttpProxyListDTO toHttpListDTO(ProxyDO proxyDO, int httpProxyPort);


    /*-------------------------------------ModelToDTO----------------------------------------------*/
    @Mapping(target = "protocol", expression = "java(ProtocolType.HTTP.getCode())")
    @Mapping(target = "bandwidth", source = "bandwidth")
    TunnelDetailDTO.HttpProxyDTO toHttpProxyDTO(ProxyConfig config);

    @Mapping(target = "protocol", expression = "java(ProtocolType.TCP.getCode())")
    @Mapping(target = "bandwidth", source = "bandwidth")
    TunnelDetailDTO.TcpProxyDTO toTcpProxyDTO(ProxyConfig config);

    @Mapping(target = "protocol", expression = "java(ProtocolType.HTTP.getCode())")
    @Mapping(target = "status", expression = "java(config.getStatus().getCode())")
    TunnelListDTO.HttpTunnelListDTO toHttpDTOList(ProxyConfig config);

    @Mapping(target = "status", expression = "java(config.getStatus().getCode())")
    @Mapping(target = "protocol", expression = "java(ProtocolType.TCP.getCode())")
    TunnelListDTO.TcpTunnelListDTO toTcpDTOList(ProxyConfig config);
}