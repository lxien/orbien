package com.xiaoniucode.etp.core.domain;

import com.xiaoniucode.etp.core.enums.HealthCheckType;
import lombok.*;


@Getter
@Setter
public class HealthCheckConfig {
    public static final Integer DEFAULT_INTERVAL = 10;
    public static final Integer DEFAULT_TIMEOUT = 8;
    public static final Integer DEFAULT_MAX_FAILED = 3;
    public static final String DEFAULT_PATH = "/health";
    /**
     * 是否开启健康检查
     */
    private boolean enabled;

    /**
     * 健康检查类型
     */
    private HealthCheckType type;

    /**
     * 检查间隔（秒）
     */
    private Integer interval = DEFAULT_INTERVAL;

    /**
     * 连接超时时间（秒）
     */
    private Integer timeout = DEFAULT_TIMEOUT;

    /**
     * 最大失败次数
     */
    private Integer maxFailed = DEFAULT_MAX_FAILED;

    /**
     * HTTP 健康检查路径
     */
    private String path = DEFAULT_PATH;
}