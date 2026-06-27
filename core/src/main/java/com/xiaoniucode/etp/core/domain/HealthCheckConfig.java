package com.xiaoniucode.etp.core.domain;

import com.xiaoniucode.etp.core.enums.HealthCheckType;
import lombok.*;

import java.util.Arrays;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@EqualsAndHashCode
public class HealthCheckConfig {
    /**
     * 是否开启健康检查
     */
    private boolean enabled = false;
    /**
     * 健康检查类型 检测目标服务采用的协议
     */
    private HealthCheckType type = HealthCheckType.AUTO;
    /**
     * 检查间隔，单位秒
     */
    private Integer interval = 10;
    /**
     * 连接超时时间，单位秒
     */
    private Integer timeout;
    /**
     * 最大失败次数
     */
    private Integer maxFailed = 3;
    /**
     * HTTP协议健康检查路径
     */
    private String path = "/health";
    private List<Integer> expectedStatus = Arrays.asList(200, 204);
}
