package io.github.lxien.orbien.core.enums;

import io.github.lxien.orbien.common.utils.StringUtils;
import lombok.Getter;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 支持的协议类型
 *
 * @author lxien
 */
public enum ProtocolType {
    TCP(1, "tcp"),
    HTTP(2, "http"),
    HTTPS(3, "https"),
    UDP(4, "udp"),
    SOCKS5(5, "socks5");
    private static final Map<Integer, ProtocolType> TYPE_MAP;
    private static final Map<String, ProtocolType> NAME_MAP;

    static {
        Map<Integer, ProtocolType> typeMap = new HashMap<>();
        Map<String, ProtocolType> nameMap = new HashMap<>();

        for (ProtocolType protocol : values()) {
            typeMap.put(protocol.code, protocol);
            nameMap.put(protocol.name().toLowerCase(), protocol);
            nameMap.put(protocol.desc.toLowerCase(), protocol);
        }

        TYPE_MAP = Collections.unmodifiableMap(typeMap);
        NAME_MAP = Collections.unmodifiableMap(nameMap);
    }

    private final int code;
    @Getter
    private final String desc;

    ProtocolType(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static ProtocolType fromCode(int type) {
        return TYPE_MAP.get(type);
    }

    public static ProtocolType fromName(String name) {
        if (!StringUtils.hasText(name)) {
            return null;
        }
        return NAME_MAP.get(name.toLowerCase());
    }

    public static ProtocolType fromName(String name, ProtocolType defaultValue) {
        ProtocolType protocol = fromName(name);
        return protocol != null ? protocol : defaultValue;
    }

    public static boolean isHttpOrHttps(ProtocolType protocolType) {
        return protocolType == HTTP || protocolType == HTTPS;
    }

    public static boolean isTcp(ProtocolType protocolType) {
        return protocolType == TCP;
    }

    public static boolean isUdp(ProtocolType protocolType) {
        return protocolType == UDP;
    }

    public static boolean isSocks5(ProtocolType protocolType) {
        return protocolType == SOCKS5;
    }

    public static boolean isHttp(ProtocolType protocolType) {
        return protocolType == HTTP;
    }

    public static boolean isHttps(ProtocolType protocolType) {
        return protocolType == HTTPS;
    }

    public static boolean isHttp(String protocol) {
        ProtocolType protocolType = fromName(protocol);
        return protocolType == HTTP;
    }

    public static boolean isHttpOrHttps(String protocol) {
        ProtocolType protocolType = fromName(protocol);
        return protocolType == HTTP || protocolType == HTTPS;
    }

    public static boolean isHttps(String protocol) {
        ProtocolType protocolType = fromName(protocol);
        return protocolType == HTTPS;
    }

    public static boolean isTcp(String protocol) {
        ProtocolType protocolType = fromName(protocol);
        return protocolType == TCP;
    }

    public static boolean isUdp(String protocol) {
        ProtocolType protocolType = fromName(protocol);
        return protocolType == UDP;
    }

    public static boolean isSocks5(String protocol) {
        ProtocolType protocolType = fromName(protocol);
        return protocolType == SOCKS5;
    }

    public boolean isHttpOrHttps() {
        return this == HTTP || this == HTTPS;
    }

    public boolean isHttps() {
        return this == HTTPS;
    }

    public boolean isHttp() {
        return this == HTTP;
    }

    public boolean isTcp() {
        return this == TCP;
    }
    public boolean isTcpOrUdp() {
        return this == TCP||this==UDP;
    }
    public boolean isUdp() {
        return this == UDP;
    }

    public boolean isSocks5() {
        return this == SOCKS5;
    }

    public int getCode() {
        return code;
    }
}