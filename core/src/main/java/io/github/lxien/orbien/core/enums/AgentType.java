package io.github.lxien.orbien.core.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Agent 类型
 */
@AllArgsConstructor
@Getter
public enum AgentType {

    /**
     * 会话型客户端（Starter、CLI）
     */
    SESSION(0, "Session Agent", true),

    /**
     * 标准客户端（独立运行的可执行程序，长期运行，支持配置持久化）
     */
    STANDALONE(1, "Standalone Agent", false),
    UNKNOWN(-1, "UNKNOWN", true);

    private final Integer code;
    private final String description;
    private final boolean temporary;

    public static AgentType fromCode(Integer code) {
        for (AgentType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        return null;
    }

    public boolean isSession() {
        return this == SESSION;
    }

    public boolean isStandalone() {
        return this == STANDALONE;
    }
}
