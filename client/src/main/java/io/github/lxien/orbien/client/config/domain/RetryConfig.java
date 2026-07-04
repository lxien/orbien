package io.github.lxien.orbien.client.config.domain;

import lombok.Getter;
import lombok.Setter;

/**
 * 重试配置
 *
 * @author liuxin
 */
@Getter
@Setter
public class RetryConfig {
    /**
     * 初始重试延迟（秒）
     */
    private Integer initialDelay = 1;
    /**
     * 最大延迟时间（秒）
     */
    private Integer maxDelay = 20;
    /**
     * 最大重试次数
     */
    private Integer maxRetries = 5;
}
