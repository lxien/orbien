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

import io.github.lxien.orbien.core.enums.DomainType;
import io.github.lxien.orbien.core.enums.ProtocolType;
import io.github.lxien.orbien.core.enums.TransportProtocol;
import io.github.lxien.orbien.server.web.dto.proxy.FileShareListDTO;
import io.github.lxien.orbien.server.web.dto.proxy.HttpProxyListDTO;
import io.github.lxien.orbien.server.web.dto.proxy.HttpsProxyListDTO;
import io.github.lxien.orbien.server.web.dto.proxy.ProxyListDTO;
import io.github.lxien.orbien.server.web.dto.proxy.Socks5ProxyListDTO;
import io.github.lxien.orbien.server.web.dto.proxy.TcpProxyListDTO;
import io.github.lxien.orbien.server.web.dto.proxy.TlsCertSummaryDTO;
import io.github.lxien.orbien.server.web.dto.proxy.UdpProxyListDTO;
import io.github.lxien.orbien.server.web.entity.AgentDO;
import io.github.lxien.orbien.server.web.entity.ProxyDO;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;

import java.util.List;

@Mapper(componentModel = "spring", imports = {ProtocolType.class, DomainType.class, TransportProtocol.class})
public interface ProxyConvert {
    @Mapping(target = "transportProtocol", ignore = true)
    Socks5ProxyListDTO toSocks5ListDTO(ProxyDO proxy);

    @Mapping(target = "transportProtocol", ignore = true)
    FileShareListDTO toFileShareListDTO(ProxyDO proxy);

    @Mapping(target = "transportProtocol", ignore = true)
    TcpProxyListDTO toTcpListDTO(ProxyDO proxy);

    @Mapping(target = "transportProtocol", ignore = true)
    UdpProxyListDTO toUdpListDTO(ProxyDO proxy);

    List<TcpProxyListDTO> toTcpDTOList(List<ProxyDO> proxies);

    List<UdpProxyListDTO> toUdpDTOList(List<ProxyDO> proxies);

    @Named("domainTypeToCode")
    static Integer domainTypeToCode(DomainType domainType) {
        return domainType != null ? domainType.getCode() : null;
    }

    @Named("codeToDomainType")
    static DomainType codeToDomainType(Integer code) {
        return DomainType.fromCode(code);
    }

    @Mapping(source = "httpProxyPort", target = "httpProxyPort")
    @Mapping(target = "transportProtocol", ignore = true)
    HttpProxyListDTO toHttpListDTO(ProxyDO proxyDO, int httpProxyPort);

    @Mapping(target = "httpsProxyPort", ignore = true)
    @Mapping(target = "tlsCertSummary", ignore = true)
    HttpsProxyListDTO toHttpsListDTO(HttpProxyListDTO base);

    default HttpsProxyListDTO toHttpsListDTO(ProxyDO proxyDO, int httpsProxyPort, AgentDO agentDO) {
        HttpProxyListDTO base = toHttpListDTO(proxyDO, httpsProxyPort);
        HttpsProxyListDTO dto = toHttpsListDTO(base);
        dto.setHttpsProxyPort(httpsProxyPort);
        enrichAgentType(dto, agentDO);
        return dto;
    }

    default void enrichAgentType(ProxyListDTO dto, AgentDO agentDO) {
        if (dto == null || agentDO == null || agentDO.getAgentType() == null) {
            return;
        }
        dto.setAgentType(agentDO.getAgentType().getCode());
    }

    default void enrichTlsCertSummary(HttpsProxyListDTO dto, TlsCertSummaryDTO summary) {
        if (dto != null) {
            dto.setTlsCertSummary(summary);
        }
    }

    @AfterMapping
    default void fillProxyListFields(ProxyDO proxy, @MappingTarget ProxyListDTO dto) {
        dto.setTransportProtocol(resolveTransportProtocolCode(proxy.getTransportProtocol()));
    }

    static Integer resolveTransportProtocolCode(TransportProtocol protocol) {
        TransportProtocol resolved = protocol != null ? protocol : TransportProtocol.TCP;
        return resolved.getCode();
    }
}