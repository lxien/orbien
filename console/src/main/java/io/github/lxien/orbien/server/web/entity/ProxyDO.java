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
package io.github.lxien.orbien.server.web.entity;

import io.github.lxien.orbien.core.enums.*;
import io.github.lxien.orbien.server.web.entity.converter.*;
import io.github.lxien.orbien.core.enums.*;
import io.github.lxien.orbien.server.web.entity.converter.*;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "proxies",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_agent_name", columnNames = {"agent_id", "name"})
        },
        indexes = {
                @Index(name = "idx_agent_id", columnList = "agent_id"),
                @Index(name = "idx_agent_name", columnList = "agent_id,name")
        }
)
public class ProxyDO {
    /**
     * 代理ID
     */
    @Id
    @Column(name = "id")
    private String id;

    @Column(name = "agent_id")
    private String agentId;
    /**
     * 代理名称
     */
    @Column(name = "name", nullable = false, length = 30)
    private String name;
    /**
     * 协议类型
     */
    @Column(name = "protocol", nullable = false)
    @Convert(converter = ProtocolTypeConverter.class)
    private ProtocolType protocol;
    /**
     * 是否启用
     *
     */
    @Column(name = "status", nullable = false)
    @Convert(converter = ProxyStatusConverter.class)
    private ProxyStatus status;
    /**
     * 配置来源类型
     * 用于区分是后台手动创建，还是客户端上报等
     */
    @Column(name = "source_type", nullable = false)
    @Convert(converter = ProxySourceTypeConverter.class)
    private ProxySourceType sourceType = ProxySourceType.MANUAL;
    /**
     * 域名类型
     */
    @Convert(converter = DomainTypeConverter.class)
    @Column(name = "domain_type")
    private DomainType domainType;
    /**
     * 远程端口
     */
    @Column(name = "remote_port")
    private Integer remotePort;
    /**
     * 实际监听的端口
     */
    @Column(name = "listen_port")
    private Integer listenPort;
    /**
     * 是否采用多路复用传输
     */
    @Column(name = "multiplex")
    private Boolean multiplex;
    /**
     * 是否加密传输
     */
    @Column(name = "encrypt")
    private Boolean encrypt;
    /**
     * 是否压缩传输
     */
    @Column(name = "compress")
    private Boolean compress;
    /**
     * 数据隧道传输协议
     */
    @Column(name = "transport_protocol")
    @Convert(converter = TransportProtocolConverter.class)
    private TransportProtocol transportProtocol;
    /**
     * 是否强制HTTPS，只有HTTPS协议有效
     */
    @Column(name = "force_https")
    private Boolean forceHttps;
    /**
     * 负载均衡策略，只有目标服务端集群部署才有效
     */
    @Convert(converter = LoadBalanceConverter.class)
    @Column(name = "load_balance_strategy")
    private LoadBalanceType loadBalanceStrategy;
    /**
     * 总带宽限制（bps）
     */
    @Column(name = "limit_total", comment = "总带宽限制（bps）")
    private Long limitTotal;
    /**
     * 入站带宽限制（bps）
     */
    @Column(name = "limit_in", comment = "入站带宽限制（bps）")
    private Long limitIn;
    /**
     * 出站带宽限制（bps）
     */
    @Column(name = "limit_out", comment = "出站带宽限制（bps）")
    private Long limitOut;
    /**
     * 是否启用 HTTP 请求抓包（Inspector）
     */
    @Column(name = "inspector_enabled")
    private Boolean inspectorEnabled;
    /**
     * 创建时间
     */
    @Column(name = "created_at")
    @CreationTimestamp
    private LocalDateTime createdAt;
    /**
     * 更新时间
     */
    @Column(name = "updated_at")
    @UpdateTimestamp
    private LocalDateTime updatedAt;
}