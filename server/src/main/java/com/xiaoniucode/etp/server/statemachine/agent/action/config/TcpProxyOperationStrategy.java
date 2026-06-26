/*
 *    Copyright 2026 xiaoniucode
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

package com.xiaoniucode.etp.server.statemachine.agent.action.config;

import com.xiaoniucode.etp.server.uid.UidGenerator;
import com.xiaoniucode.etp.core.domain.ProxyConfig;
import com.xiaoniucode.etp.server.exceptions.EtpException;
import com.xiaoniucode.etp.server.exceptions.PortConflictException;
import com.xiaoniucode.etp.server.manager.ProxyManager;
import com.xiaoniucode.etp.server.port.PortManager;
import com.xiaoniucode.etp.server.statemachine.agent.AgentInfo;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class TcpProxyOperationStrategy implements ProxyConfigOperationStrategy {
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(TcpProxyOperationStrategy.class);
    @Autowired
    private PortManager portManager;
    @Autowired
    private ProxyManager proxyManager;
    @Autowired
    private UidGenerator uidGenerator;

    @Override
    public ProxyOperationResult create(ProxyConfig config, AgentInfo agentInfo) {
        logger.debug("创建TCP代理: {}", config.getName());
        Integer remotePort = config.getRemotePort();
        if (remotePort == null || remotePort == 0) {
            Integer listenPort = portManager.acquire();
            if (listenPort == null) {
                throw new EtpException("没有可用的端口");
            }
            config.setListenPort(listenPort);
            logger.debug("TCP代理 {} 自动分配端口: {}", config.getName(), listenPort);
        } else {
            if (!portManager.isAvailable(remotePort)) {
                throw new PortConflictException(remotePort);
            }
            config.setListenPort(remotePort);
            portManager.addPort(remotePort);
        }
        String proxyId = uidGenerator.getUIDAsString();
        config.setProxyId(proxyId);
        proxyManager.activate(config);
        logger.debug("TCP代理 {} 注册成功，监听端口: {}", config.getName(), config.getListenPort());
        return new ProxyOperationResult(null, config.getListenPort(), true);
    }

    @Override
    public ProxyOperationResult update(ProxyConfig newConfig, ProxyConfig oldConfig, AgentInfo agentInfo) {
        if (oldConfig.getProtocol() != newConfig.getProtocol()) {
            logger.debug("TCP代理更新 {} 协议类型发生变化，旧: {}, 新: {}",
                    newConfig.getName(), oldConfig.getProtocol().name(), newConfig.getProtocol().name());
            proxyManager.deactivate(oldConfig.getProxyId());
            return create(newConfig, agentInfo);
        } else {
            logger.debug("TCP代理更新 {} 协议类型未发生变化", newConfig.getName());
            Integer oldRemotePort = oldConfig.getRemotePort();
            Integer newRemotePort = newConfig.getRemotePort();
            //端口发生变化
            if (newRemotePort != null && !newRemotePort.equals(oldRemotePort)) {
                logger.debug("TCP代理更新 {} 远程端口配置发生变化，旧: {}, 新: {}", newConfig.getName(), oldRemotePort, newRemotePort);
                if (!portManager.isAvailable(newRemotePort)) {
                    throw new PortConflictException(newRemotePort);
                }
                newConfig.setListenPort(newRemotePort);
                portManager.addPort(newRemotePort);
            } else {
                newConfig.setRemotePort(oldConfig.getListenPort());
            }
            proxyManager.reconcile(newConfig);
            return new ProxyOperationResult(null, newConfig.getListenPort(), true);
        }
    }

    @Override
    public boolean supports(ProxyConfig config) {
        return config.isTcp();
    }
}
