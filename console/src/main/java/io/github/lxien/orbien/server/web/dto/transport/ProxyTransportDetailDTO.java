package io.github.lxien.orbien.server.web.dto.transport;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 代理传输配置详情
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ProxyTransportDetailDTO extends TransportDTO {
    /**
     * 数据隧道传输协议：1 TCP，2 WebSocket，3 QUIC
     */
    private Integer dataProtocol;
    /**
     * 运行时实际生效的传输协议（未配置时默认为 TCP）
     */
    private Integer effectiveDataProtocol;
    private Boolean effectiveEncrypt;
    private Integer effectiveTunnelType;
    private TransportEncryptConstraints encryptConstraints;
    private TransportTunnelConstraints tunnelConstraints;
    private TransportProtocolConstraints protocolConstraints;
    private TransportCompressConstraints compressConstraints;
    private Boolean effectiveCompress;
    private String effectiveCompressAlgorithm;
}
