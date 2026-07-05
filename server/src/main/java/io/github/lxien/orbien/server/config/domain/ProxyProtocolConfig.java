package io.github.lxien.orbien.server.config.domain;

import lombok.Data;

import java.util.HashSet;
import java.util.Set;

/**
 * 访客入口 PROXY 协议（HAProxy/LB 前置）配置
 */
@Data
public class ProxyProtocolConfig {
    /**
     * 是否在 HTTP/HTTPS/TCP 访客 pipeline 最前插入 HAProxy 解码器
     */
    private boolean enabled = false;
    /**
     * true=无 PROXY 头则拒绝；false=允许直连（开发环境）
     */
    private boolean strict = false;
    /**
     * 仅当 immediate peer（通常是 LB IP）命中时才信任 PROXY 头；空=信任所有
     */
    private Set<String> trustedProxies = new HashSet<>();
}
