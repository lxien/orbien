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

package io.github.lxien.orbien.server.manager;

import io.github.lxien.orbien.server.exceptions.OrbienException;
import io.github.lxien.orbien.server.metrics.MetricsCollector;
import io.github.lxien.orbien.server.port.PortAcceptor;
import io.github.lxien.orbien.server.port.UdpPortAcceptor;
import io.github.lxien.orbien.core.enums.PortPoolType;
import io.github.lxien.orbien.server.port.PortPoolManager;
import io.github.lxien.orbien.server.security.IpAccessChecker;
import io.github.lxien.orbien.server.statemachine.stream.StreamManager;
import io.github.lxien.orbien.server.transport.https.TlsCertificateManager;
import io.github.lxien.orbien.server.vhost.DomainRegistry;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 运行时代理管理器，负责代理的注册、注销和状态变更等操作。
 */
@Component
public class ProxyManager {
    private final InternalLogger logger = InternalLoggerFactory.getInstance(ProxyManager.class);
    private final Map<String/*proxyId*/, Integer/*listenPort*/> portMap = new ConcurrentHashMap<>();
    private final Map<String/*proxyId*/, Integer/*listenPort*/> udpPortMap = new ConcurrentHashMap<>();
    private final Map<String/*agentId*/, Set<String/*proxyId*/>> agentProxyMap = new ConcurrentHashMap<>();
    private final Map<String/*proxyId*/, String/*agentId*/> proxyAgentMap = new ConcurrentHashMap<>();
    @Autowired
    private MetricsCollector metricsCollector;
    @Autowired
    private IpAccessChecker ipAccessChecker;
    @Autowired
    private PortAcceptor portAcceptor;
    @Autowired
    private UdpPortAcceptor udpPortAcceptor;
    @Autowired
    private PortPoolManager portPoolManager;
    @Autowired
    private DomainRegistry domainRegistry;
    @Autowired
    private StreamManager streamManager;
    @Autowired
    private TlsCertificateManager tlsCertificateManager;
    private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();
    private final Lock readLock = rwLock.readLock();
    private final Lock writeLock = rwLock.writeLock();

    public void registerTcp(String agentId, String proxyId, Integer listenPort) throws OrbienException {
        if (agentId == null || proxyId == null || listenPort == null) {
            throw new IllegalArgumentException("无效输入参数");
        }
        logger.debug("激活TCP代理: {}", proxyId);
        writeLock.lock();
        try {
            proxyAgentMap.put(proxyId, agentId);
            agentProxyMap.computeIfAbsent(agentId, k -> ConcurrentHashMap.newKeySet()).add(proxyId);
            portMap.put(proxyId, listenPort);
            portAcceptor.bindPort(listenPort);
        } finally {
            writeLock.unlock();
        }
    }

    public void registerUdp(String agentId, String proxyId, Integer listenPort) throws OrbienException {
        if (agentId == null || proxyId == null || listenPort == null) {
            throw new IllegalArgumentException("无效输入参数");
        }
        logger.debug("激活 UDP 代理: {}", proxyId);
        writeLock.lock();
        try {
            proxyAgentMap.put(proxyId, agentId);
            agentProxyMap.computeIfAbsent(agentId, k -> ConcurrentHashMap.newKeySet()).add(proxyId);
            udpPortMap.put(proxyId, listenPort);
            udpPortAcceptor.bindPort(listenPort);
        } finally {
            writeLock.unlock();
        }
    }

    public void registerHttp(String agentId, String proxyId, Set<String> domains) throws OrbienException {
        if (agentId == null || proxyId == null) {
            throw new IllegalArgumentException("无效输入参数");
        }
        if (CollectionUtils.isEmpty(domains)) {
            throw new IllegalArgumentException("至少配置一个域名");
        }
        logger.debug("激活HTTP代理: {}", proxyId);
        writeLock.lock();
        try {
            proxyAgentMap.put(proxyId, agentId);
            agentProxyMap.computeIfAbsent(agentId, k -> ConcurrentHashMap.newKeySet()).add(proxyId);
            domainRegistry.register(proxyId, domains);
        } finally {
            writeLock.unlock();
        }
    }

    public void registerHttps(String agentId, String proxyId, Set<String> domains) throws OrbienException {
        if (agentId == null || proxyId == null) {
            throw new IllegalArgumentException("无效输入参数");
        }
        if (CollectionUtils.isEmpty(domains)) {
            throw new IllegalArgumentException("至少配置一个域名");
        }
        logger.debug("激活HTTPS代理: {}", proxyId);
        writeLock.lock();
        try {
            proxyAgentMap.put(proxyId, agentId);
            agentProxyMap.computeIfAbsent(agentId, k -> ConcurrentHashMap.newKeySet()).add(proxyId);
            domainRegistry.register(proxyId, domains);
            //todo tlsCertificateManager.addDeployedDomains(domains);
        } finally {
            writeLock.unlock();
        }
    }

    private void doDeactivate(String proxyId) {
        if (!StringUtils.hasText(proxyId)) {
            throw new IllegalArgumentException("proxyId 不能为空");
        }
        logger.debug("停用代理: {}", proxyId);

        // TCP 协议
        Integer listenPort = portMap.remove(proxyId);
        if (listenPort != null) {
            shutdownTcpPortResources(listenPort);
        }
        Integer udpListenPort = udpPortMap.remove(proxyId);
        if (udpListenPort != null) {
            shutdownUdpPortResources(udpListenPort);
        }
        String agentId = proxyAgentMap.remove(proxyId);
        if (StringUtils.hasText(agentId)) {
            Set<String> set = agentProxyMap.get(agentId);
            if (!CollectionUtils.isEmpty(set)) {
                set.remove(proxyId);
            }
        }
        // HTTP(S)协议
        Set<String> domains = domainRegistry.getDomainsByProxyId(proxyId);
        for (String domain : domains) {
           //todo  tlsCertificateManager.cancelDeploy(domain);// HTTPS协议
            streamManager.fireCloseByDomain(domain);
        }
        //从注册中心删除域名
        domainRegistry.unregister(proxyId);
        //删除IP访问控制
        ipAccessChecker.invalidate(proxyId);
        //删除代理流量最近内存统计记录
        metricsCollector.removeByProxyId(proxyId);
    }

    public void deactivates(List<String> proxyIds) {
        if (CollectionUtils.isEmpty(proxyIds)) {
            return;
        }
        writeLock.lock();
        try {
            for (String proxyId : proxyIds) {
                doDeactivate(proxyId);
            }
        } finally {
            writeLock.unlock();
        }
    }

    public void deactivate(String proxyId) {
        if (!StringUtils.hasText(proxyId)) {
            throw new IllegalArgumentException("proxyId 不能为空");
        }
        writeLock.lock();
        try {
            doDeactivate(proxyId);
        } finally {
            writeLock.unlock();
        }
    }

    public void onAgentOffline(String agentId) throws OrbienException {
        writeLock.lock();
        try {
            Set<String> proxyIds = agentProxyMap.remove(agentId);
            if (CollectionUtils.isEmpty(proxyIds)) {
                return;
            }
            logger.info("清理 {} 个代理: agentId={}", proxyIds.size(), agentId);
            List<String> idsToRemove = new ArrayList<>(proxyIds);
            for (String proxyId : idsToRemove) {
                doDeactivate(proxyId);
            }
        } finally {
            writeLock.unlock();
        }
    }

    private void shutdownTcpPortResources(int listenPort) {
        portPoolManager.release(PortPoolType.TCP, listenPort);
        portAcceptor.stopPortListen(listenPort);
        streamManager.fireCloseByPort(listenPort);
    }

    private void shutdownUdpPortResources(int listenPort) {
        portPoolManager.release(PortPoolType.UDP, listenPort);
        udpPortAcceptor.stopPortListen(listenPort);
        streamManager.fireCloseByUdpPort(listenPort);
    }
}