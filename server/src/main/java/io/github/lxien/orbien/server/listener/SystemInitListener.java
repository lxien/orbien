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

package io.github.lxien.orbien.server.listener;

import io.github.lxien.orbien.core.enums.ProtocolType;
import io.github.lxien.orbien.server.notify.EventBus;
import io.github.lxien.orbien.server.notify.EventListener;
import io.github.lxien.orbien.server.event.TunnelServerBindEvent;
import io.github.lxien.orbien.core.enums.PortPoolType;
import io.github.lxien.orbien.server.port.PortPoolManager;
import io.github.lxien.orbien.server.service.ProxyConfigService;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 系统初始化
 */
@Component
public class SystemInitListener implements EventListener<TunnelServerBindEvent> {
    private final InternalLogger logger = InternalLoggerFactory.getInstance(SystemInitListener.class);
    @Autowired
    private EventBus eventBus;
    @Autowired
    private ProxyConfigService proxyConfigService;
    @Autowired
    private PortPoolManager portPoolManager;

    @PostConstruct
    public void init() {
        eventBus.register(this);
    }

    @Override
    public void onEvent(TunnelServerBindEvent event) {
        logger.debug("回填已占用端口");
        List<Integer> tcpPorts = proxyConfigService.getListenPorts(ProtocolType.TCP);
        for (Integer port : tcpPorts) {
            portPoolManager.markAllocated(PortPoolType.TCP, port);
        }
        List<Integer> socks5Ports = proxyConfigService.getListenPorts(ProtocolType.SOCKS5);
        for (Integer port : socks5Ports) {
            portPoolManager.markAllocated(PortPoolType.TCP, port);
        }
        List<Integer> udpPorts = proxyConfigService.getListenPorts(ProtocolType.UDP);
        for (Integer port : udpPorts) {
            portPoolManager.markAllocated(PortPoolType.UDP, port);
        }
    }
}
