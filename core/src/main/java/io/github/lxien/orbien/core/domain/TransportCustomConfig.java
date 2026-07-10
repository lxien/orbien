package io.github.lxien.orbien.core.domain;

import io.github.lxien.orbien.core.enums.TransportProtocol;
import io.github.lxien.orbien.core.transport.compress.CompressionType;
import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class TransportCustomConfig {
    private TransportProtocol protocol;
    private Boolean multiplex;
    private Boolean encrypt;
    private Boolean compress;
    private CompressionType compressAlgorithm;

    /**
     * 解析当前代理应使用的压缩算法；未开启压缩时返回 {@link CompressionType#NONE}。
     */
    public CompressionType resolveCompressAlgorithm() {
        if (!Boolean.TRUE.equals(compress)) {
            return CompressionType.NONE;
        }
        return compressAlgorithm != null ? compressAlgorithm : CompressionType.SNAPPY;
    }
}
