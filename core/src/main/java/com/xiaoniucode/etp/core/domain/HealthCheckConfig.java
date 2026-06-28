package com.xiaoniucode.etp.core.domain;

import com.xiaoniucode.etp.core.enums.HealthCheckType;
import lombok.*;

import java.util.Arrays;
import java.util.List;

/**
 * 健康检查配置
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@EqualsAndHashCode
public class HealthCheckConfig {
    public static final boolean DEFAULT_ENABLED = false;
    public static final HealthCheckType DEFAULT_TYPE = HealthCheckType.AUTO;
    public static final Integer DEFAULT_INTERVAL = 10;
    public static final Integer DEFAULT_TIMEOUT = 8;
    public static final Integer DEFAULT_MAX_FAILED = 3;
    public static final String DEFAULT_PATH = "/health";
    public static final List<Integer> DEFAULT_EXPECTED_STATUS = Arrays.asList(200, 204);

    /**
     * 是否开启健康检查
     */
    private boolean enabled = DEFAULT_ENABLED;

    /**
     * 健康检查类型
     */
    private HealthCheckType type = DEFAULT_TYPE;

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

    /**
     * 期望的 HTTP 状态码
     */
    private List<Integer> expectedStatus = DEFAULT_EXPECTED_STATUS;
}