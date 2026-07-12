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
package io.github.lxien.orbien.core.domain;

import io.github.lxien.orbien.core.enums.*;
import io.github.lxien.orbien.core.http.ForceHttpsPolicy;
import io.github.lxien.orbien.core.transport.api.TransportEndpointResolver;
import io.github.lxien.orbien.core.transport.api.TransportEncryptResolver;
import io.github.lxien.orbien.core.transport.compress.CompressionType;
import io.github.lxien.orbien.core.enums.TransportProtocol;

import lombok.*;

import java.io.Serializable;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Getter
@ToString
@EqualsAndHashCode
public class ProxyConfig implements Serializable {
    @Setter
    private String agentId;
    @Setter
    private AgentType agentType;
    /**
     * 代理ID 唯一标识
     */
    @Setter
    private String proxyId;
    /**
     * 代理名称
     */
    @Setter
    private String name;
    /**
     * 协议类型
     */
    @Setter
    private ProtocolType protocol;
    /**
     * 代理配置来源
     */
    @Setter
    private ProxySourceType sourceType;
    /**
     * TCP 代理 远程端口
     */
    @Setter
    private Integer remotePort;
    /**
     * 实际监听的端口
     */
    @Setter
    private Integer listenPort;
    /**
     * 是否强制HTTPS 访问，仅对HTTPS协议有效
     */
    @Setter
    private Boolean forceHttps;
    /**
     * 代理目标服务
     */
    private final List<Target> targets = new CopyOnWriteArrayList<>();
    /**
     * 代理状态
     */
    @Setter
    private ProxyStatus status;
    /**
     * 负载均衡配置
     */
    @Setter
    private LoadBalanceType loadBalanceType;
    /**
     * HTTP(s) 域名配置
     */
    @Setter
    private RouteConfig routeConfig;
    /**
     * HTTPS TLS 证书配置
     */
    @Setter
    private ProxyTlsCertConfig tlsCertConfig;
    /**
     * IP 防火墙
     */
    @Setter
    private AccessControlConfig accessControl;
    /**
     * HTTP Basic Auth配置
     */
    @Setter
    private BasicAuthConfig basicAuth;
    /**
     * 带宽限制配置
     */
    @Setter
    private BandwidthConfig bandwidth;

    /**
     * 传输配置
     */
    @Setter
    private TransportCustomConfig transport;
    /**
     * 健康检查
     */
    @Setter
    private HealthCheckConfig healthCheck;

    /**
     * SOCKS5 认证配置
     */
    @Setter
    private Socks5AuthConfig socks5Auth;

    /**
     * 文件共享认证配置
     */
    @Setter
    private FileShareAuthConfig fileShareAuth;

    /**
     * 文件共享限制配置
     */
    @Setter
    private FileShareLimitsConfig fileShareLimits;

    /**
     * 是否启用 HTTP 请求抓包（Inspector）
     */
    @Setter
    private Boolean inspectorEnabled;

    public boolean isInspectorEnabled() {
        return Boolean.TRUE.equals(inspectorEnabled);
    }

    public boolean hasTransport() {
        return transport != null;
    }

    /**
     * 是否启用加密
     */
    public boolean isEncrypt() {
        return transport != null && Boolean.TRUE.equals(transport.getEncrypt());
    }

    /**
     * 结合数据隧道协议与全局 TLS 开关，解析传输层加密最终生效值。
     */
    public boolean resolveEffectiveEncrypt(boolean globalTlsEnabled) {
        TransportProtocol dataProtocol = getTransportProtocol(TransportProtocol.TCP);
        Boolean requested = transport != null ? transport.getEncrypt() : null;
        return TransportEncryptResolver.resolveEffectiveEncrypt(dataProtocol, globalTlsEnabled, requested);
    }

    /**
     * 是否启用压缩
     */
    public boolean isCompress() {
        return transport != null && Boolean.TRUE.equals(transport.getCompress());
    }

    public CompressionType resolveCompressAlgorithm() {
        if (transport == null) {
            return CompressionType.NONE;
        }
        return transport.resolveCompressAlgorithm();
    }

    public boolean hasAccessControl() {
        return accessControl != null;
    }

    public boolean hasBandwidthLimit() {
        return bandwidth != null;
    }

    public boolean hasBasicAuth() {
        return basicAuth != null;
    }

    public boolean hasSocks5Auth() {
        return socks5Auth != null;
    }

    public boolean hasFileShareAuth() {
        return fileShareAuth != null;
    }

    public boolean hasFileShareLimits() {
        return fileShareLimits != null;
    }

    /**
     * 是否是 HTTP 协议
     */
    public boolean isHttp() {
        return ProtocolType.isHttp(protocol);
    }

    /**
     * 是否是 HTTPS 协议
     */
    public boolean isHttps() {
        return ProtocolType.isHttps(protocol);
    }

    /**
     * HTTPS / 文件共享是否开启 HTTP→HTTPS 强制跳转（文件共享固定 true；HTTPS 未配置时默认 true）。
     */
    public boolean isForceHttpsEnabled() {
        return ForceHttpsPolicy.isRedirectEnabled(this);
    }

    public boolean isHttpOrHttps() {
        return ProtocolType.isHttp(protocol) || ProtocolType.isHttps(protocol);
    }

    /**
     * 是否是 TCP 协议
     */
    public boolean isTcp() {
        return ProtocolType.isTcp(protocol);
    }

    /**
     * 是否是 UDP 协议
     */
    public boolean isUdp() {
        return ProtocolType.isUdp(protocol);
    }

    public boolean isSocks5() {
        return ProtocolType.isSocks5(protocol);
    }

    public boolean isFile() {
        return ProtocolType.isFile(protocol);
    }

    public boolean hasRemotePort() {
        return remotePort != null;
    }

    /**
     * 是否需要使用负载均衡
     */
    public boolean isLoadBalanceNeeded() {
        return targets.size() > 1;
    }

    public boolean addTarget(Target target) {
        if (target == null) {
            return false;
        }
        if (targets.contains(target)) {
            return false;
        }
        targets.add(target);
        return true;
    }

    public int addTargets(List<Target> newTargets) {
        if (newTargets == null || newTargets.isEmpty()) {
            return 0;
        }
        int addedCount = 0;
        for (Target target : newTargets) {
            if (addTarget(target)) {
                addedCount++;
            }
        }
        return addedCount;
    }

    public boolean isMuxTunnel() {
        return transport != null && Boolean.TRUE.equals(transport.getMultiplex());
    }

    public TransportProtocol getTransportProtocol(TransportProtocol globalDefault) {
        return TransportEndpointResolver.resolveDataProtocol(globalDefault, transport);
    }

    public boolean isMuxTunnelFor(TransportProtocol protocol) {
        if (transport != null && transport.getMultiplex() != null) {
            return TransportEndpointResolver.normalizeMultiplex(protocol, transport.getMultiplex());
        }
        return isMuxTunnel();
    }

}

