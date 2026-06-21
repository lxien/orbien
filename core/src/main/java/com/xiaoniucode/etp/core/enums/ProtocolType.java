package com.xiaoniucode.etp.core.enums;

import com.xiaoniucode.etp.common.utils.StringUtils;
import lombok.Getter;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 支持的协议类型
 *
 * @author liuxin
 */
public enum ProtocolType {
    TCP(1, "tcp"),
    HTTP(2, "http"),
    HTTPS(3, "https");
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

    public static ProtocolType getByName(String name) {
        if (!StringUtils.hasText(name)) {
            return null;
        }
        return NAME_MAP.get(name.toLowerCase());
    }

    public static ProtocolType getByName(String name, ProtocolType defaultValue) {
        ProtocolType protocol = getByName(name);
        return protocol != null ? protocol : defaultValue;
    }

    public static boolean isTcp(ProtocolType protocolType) {
        return protocolType == TCP;
    }

    public static boolean isHttp(ProtocolType protocolType) {
        return protocolType == HTTP;
    }

    public static boolean isHttp(String protocol) {
        ProtocolType protocolType = getByName(protocol);
        return protocolType == HTTP;
    }

    public static boolean isTcp(String protocol) {
        ProtocolType protocolType = getByName(protocol);
        return protocolType == TCP;
    }

    public int getCode() {
        return code;
    }

    public boolean isHttp() {
        return this ==HTTP;
    }
    public boolean isTcp() {
        return this ==TCP;
    }
}