package io.github.lxien.orbien.autoconfigure;

import io.github.lxien.orbien.core.enums.TransportProtocol;
import lombok.Data;

/**
 * 代理级自定义传输配置
 */
@Data
public class TransportCustomProperties {
    private TransportProtocol protocol;
    private boolean encrypt;
    private boolean multiplex = true;
    private boolean compress;
    private String compressAlgorithm = "snappy";
}
