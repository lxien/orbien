package io.github.lxien.orbien.core.enums;

import lombok.Getter;

@Getter
public enum LoadBalanceType {

    /**
     * 轮询 - 按顺序轮流分配
     */
    ROUND_ROBIN(1, "roundrobin", "轮询"),

    /**
     * 加权轮询 - 按权重比例分配
     */
    WEIGHT(2, "weight", "加权轮询"),

    /**
     * 随机
     */
    RANDOM(3, "random", "随机"),

    /**
     * 最少连接 - 分配给当前连接数最少的后端
     */
    LEAST_CONN(4, "leastconn", "最少连接");

    private final Integer code;
    private final String name;
    private final String description;

    LoadBalanceType(Integer code, String name, String description) {
        this.code = code;
        this.name = name;
        this.description = description;
    }

    public static LoadBalanceType fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (LoadBalanceType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        return null;
    }

    public static LoadBalanceType fromName(String name) {
        if (name == null) {
            return null;
        }
        for (LoadBalanceType strategy : values()) {
            if (strategy.name.equalsIgnoreCase(name)) {
                return strategy;
            }
        }
        return null;
    }
}