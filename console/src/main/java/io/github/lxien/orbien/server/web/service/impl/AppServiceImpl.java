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

package io.github.lxien.orbien.server.web.service.impl;

import io.github.lxien.orbien.core.domain.transport.QuicProtocolConfig;
import io.github.lxien.orbien.core.domain.transport.WebSocketProtocolConfig;
import io.github.lxien.orbien.server.config.AppConfig;
import io.github.lxien.orbien.server.config.domain.TransportConfig;
import io.github.lxien.orbien.server.web.dto.app.AppConfigInfoDTO;
import io.github.lxien.orbien.server.web.service.AppService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

@Service
public class AppServiceImpl implements AppService {
    @Resource
    private AppConfig appConfig;

    @Override
    public AppConfigInfoDTO getAppConfigInfo() {
        AppConfigInfoDTO dto = new AppConfigInfoDTO();
        dto.setServerAddr(appConfig.getServerAddr());
        dto.setServerPort(appConfig.getServerPort());
        if (appConfig.getRootDomains() != null && !appConfig.getRootDomains().isEmpty()) {
            dto.setRootDomain(appConfig.getRootDomains().iterator().next());
        }
        dto.setHttpProxyPort(appConfig.getHttpProxyPort());
        dto.setHttpsProxyPort(appConfig.getHttpsProxyPort());

        TransportConfig transportConfig = appConfig.getTransportConfig();
        if (transportConfig != null) {
            WebSocketProtocolConfig websocket = transportConfig.getWebsocket();
            if (websocket != null) {
                dto.setWebsocketEnabled(websocket.isEnabled());
                if (websocket.isEnabled()) {
                    dto.setWebsocketPort(websocket.getPort());
                }
            }
            QuicProtocolConfig quic = transportConfig.getQuic();
            if (quic != null) {
                dto.setQuicEnabled(quic.isEnabled());
                if (quic.isEnabled()) {
                    dto.setQuicPort(quic.getPort());
                }
            }
        }
        return dto;
    }
}
