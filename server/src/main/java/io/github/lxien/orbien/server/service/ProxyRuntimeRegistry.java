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

package io.github.lxien.orbien.server.service;

import io.github.lxien.orbien.core.domain.DomainInfo;
import io.github.lxien.orbien.core.domain.ProxyConfig;
import io.github.lxien.orbien.core.domain.ProxyConfigExt;
import io.github.lxien.orbien.server.manager.ProxyManager;
import io.github.lxien.orbien.server.service.repository.ProxyQueryRepository;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 将数据库中的已启用代理注册到运行时（域名索引、端口监听等）。
 */
@Service
public class ProxyRuntimeRegistry {
    private final InternalLogger logger = InternalLoggerFactory.getInstance(ProxyRuntimeRegistry.class);

    @Autowired
    private ProxyQueryRepository proxyQueryRepository;
    @Autowired
    private ProxyManager proxyManager;

    public void registerAllOpen() {
        List<ProxyConfigExt> proxies = proxyQueryRepository.findAllOpen();
        if (CollectionUtils.isEmpty(proxies)) {
            logger.debug("无已启用代理需要回填运行时注册");
            return;
        }
        int success = 0;
        for (ProxyConfigExt ext : proxies) {
            if (registerOpen(ext)) {
                success++;
            }
        }
        logger.info("运行时代理回填完成: 成功 {}/{}", success, proxies.size());
    }

    public void registerByAgentId(String agentId) {
        if (!StringUtils.hasText(agentId)) {
            return;
        }
        List<ProxyConfigExt> proxies = proxyQueryRepository.findByAgentId(agentId);
        if (CollectionUtils.isEmpty(proxies)) {
            return;
        }
        for (ProxyConfigExt ext : proxies) {
            registerOpen(ext);
        }
    }

    public boolean registerOpen(ProxyConfigExt configExt) {
        if (configExt == null || configExt.getProxyConfig() == null) {
            return false;
        }
        ProxyConfig config = configExt.getProxyConfig();
        if (!config.getStatus().isOpen()) {
            return false;
        }
        try {
            if (config.isHttpOrHttps()) {
                Set<String> domains = toDomainSet(configExt);
                if (CollectionUtils.isEmpty(domains)) {
                    logger.warn("代理 {} 未配置域名，跳过运行时注册", config.getName());
                    return false;
                }
                if (config.isHttp()) {
                    proxyManager.registerHttp(config.getAgentId(), config.getProxyId(), domains);
                    logger.debug("注册HTTP代理: {}", config.getName());
                } else {
                    proxyManager.registerHttps(config.getAgentId(), config.getProxyId(), domains);
                    logger.debug("注册HTTPS代理: {}", config.getName());
                }
            } else if (config.isFile()) {
                Set<String> domains = toDomainSet(configExt);
                if (CollectionUtils.isEmpty(domains)) {
                    logger.warn("文件共享 {} 未配置域名，跳过运行时注册", config.getName());
                    return false;
                }
                proxyManager.registerFile(config.getAgentId(), config.getProxyId(), domains);
                logger.debug("注册文件共享: {}", config.getName());
            } else if (config.isUdp()) {
                proxyManager.registerUdp(config.getAgentId(), config.getProxyId(), config.getListenPort());
                logger.debug("注册UDP代理: {}", config.getName());
            } else if (config.isTcp()) {
                proxyManager.registerTcp(config.getAgentId(), config.getProxyId(), config.getListenPort());
                logger.debug("注册TCP代理: {}", config.getName());
            } else if (config.isSocks5()) {
                proxyManager.registerSocks5(config.getAgentId(), config.getProxyId(), config.getListenPort());
                logger.debug("注册SOCKS5代理: {}", config.getName());
            } else {
                return false;
            }
            return true;
        } catch (RuntimeException e) {
            logger.warn("注册代理失败: proxyId={}, name={}, reason={}",
                    config.getProxyId(), config.getName(), e.getMessage());
            return false;
        }
    }

    private Set<String> toDomainSet(ProxyConfigExt configExt) {
        return configExt.getDomains().stream()
                .map(DomainInfo::getFullDomain)
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet());
    }
}
