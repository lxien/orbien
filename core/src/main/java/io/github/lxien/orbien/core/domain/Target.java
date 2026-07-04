package io.github.lxien.orbien.core.domain;

import lombok.*;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class Target {
    /**
     * 目标 主机 地址
     */
    private String host;

    /**
     * 目标端口
     */
    private Integer port;

    /**
     * 权重（用于加权轮询）
     * 默认值: 1
     */
    private Integer weight = 1;

    /**
     * 目标代理名称
     */
    private String name;

    public Target(String host, Integer port) {
        this.host = host;
        this.port = port;
    }
}
