package io.github.lxien.orbien.core.enums;

import io.github.lxien.orbien.common.utils.StringUtils;
import lombok.Getter;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 数据隧道传输协议（控制/数据通道）
 */
@Getter
public enum TransportProtocol {
    TCP(1, "tcp", 9527, true, true),
    WEBSOCKET(2, "websocket", 9528, true, true),
    QUIC(3, "quic", 9529, true, false);

    private static final Map<Integer, TransportProtocol> CODE_MAP;
    private static final Map<String, TransportProtocol> NAME_MAP;

    static {
        Map<Integer, TransportProtocol> codeMap = new HashMap<>();
        Map<String, TransportProtocol> nameMap = new HashMap<>();
        for (TransportProtocol protocol : values()) {
            codeMap.put(protocol.code, protocol);
            nameMap.put(protocol.name.toLowerCase(), protocol);
            nameMap.put(protocol.name().toLowerCase(), protocol);
        }
        CODE_MAP = Collections.unmodifiableMap(codeMap);
        NAME_MAP = Collections.unmodifiableMap(nameMap);
    }

    private final int code;
    private final String name;
    private final int defaultPort;
    private final boolean supportsMultiplex;
    private final boolean supportsDirect;

    TransportProtocol(int code, String name, int defaultPort, boolean supportsMultiplex, boolean supportsDirect) {
        this.code = code;
        this.name = name;
        this.defaultPort = defaultPort;
        this.supportsMultiplex = supportsMultiplex;
        this.supportsDirect = supportsDirect;
    }

    public static TransportProtocol fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        return CODE_MAP.get(code);
    }

    public static TransportProtocol fromName(String name) {
        if (!StringUtils.hasText(name)) {
            return null;
        }
        return NAME_MAP.get(name.trim().toLowerCase());
    }

    public static TransportProtocol fromName(String name, TransportProtocol defaultValue) {
        TransportProtocol protocol = fromName(name);
        return protocol != null ? protocol : defaultValue;
    }

    /**
     * 线上帧编码仍使用 ordinal，保持与既有客户端/服务端兼容。
     */
    public byte toWire() {
        return (byte) ordinal();
    }

    public static TransportProtocol fromWire(byte value) {
        for (TransportProtocol protocol : values()) {
            if (protocol.ordinal() == value) {
                return protocol;
            }
        }
        return TCP;
    }
}
