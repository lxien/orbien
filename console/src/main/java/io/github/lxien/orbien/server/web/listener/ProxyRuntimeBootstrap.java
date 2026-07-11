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

package io.github.lxien.orbien.server.web.listener;

import io.github.lxien.orbien.server.notify.EventBus;
import io.github.lxien.orbien.server.notify.EventListener;
import io.github.lxien.orbien.server.event.TunnelServerBindEvent;
import io.github.lxien.orbien.server.service.ProxyRuntimeRegistry;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 服务启动后将数据库中已启用的代理回填到运行时注册中心（域名、端口等）。
 */
@Component
public class ProxyRuntimeBootstrap implements EventListener<TunnelServerBindEvent> {
    private static final Logger logger = LoggerFactory.getLogger(ProxyRuntimeBootstrap.class);

    @Autowired
    private EventBus eventBus;
    @Autowired
    private ProxyRuntimeRegistry proxyRuntimeRegistry;

    @PostConstruct
    public void init() {
        eventBus.register(this);
    }

    @Override
    public void onEvent(TunnelServerBindEvent event) {
        logger.debug("开始回填已启用代理的运行时注册");
        proxyRuntimeRegistry.registerAllOpen();
    }
}
