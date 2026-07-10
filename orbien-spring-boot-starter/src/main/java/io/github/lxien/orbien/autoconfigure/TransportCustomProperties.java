package io.github.lxien.orbien.autoconfigure;

import io.github.lxien.orbien.core.enums.TransportProtocol;
import lombok.Data;

/**
 * 代理级传输配置
 */
@Data
public class TransportCustomProperties {
    /**
     * 自定义数据传输协议（可选）。未配置时使用全局 {@code orbien.client.transport.protocol}。
     */
    private TransportProtocol protocol;
    private boolean encrypt;
    private boolean multiplex = true;
    private boolean compress;
    private String compressAlgorithm = "snappy";
}
