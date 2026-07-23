package io.github.lxien.orbien.server.inspector;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;

/**
 * Inspector 流量抓包全局配置
 */
@Getter
@Setter
@Component
public class InspectorProperties {
    private boolean enabled = true;
    /**
     * 每代理内存环形缓冲硬上限
     */
    private int maxRecordsPerProxy = 100;
    /**
     * 列表 API 默认返回条数
     */
    private int defaultListLimit = 50;
    private int maxBodyBytes = 65536;
    /**
     * 单次 replay 等待响应超时（毫秒）
     */
    private long replayTimeoutMs = 30_000;
    /**
     * 同时进行的 replay 上限
     */
    private int replayMaxConcurrency = 16;

    public int clampListLimit(int limit) {
        if (limit <= 0) {
            return defaultListLimit;
        }
        return Math.min(limit, maxRecordsPerProxy);
    }
}
