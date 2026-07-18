package io.github.lxien.orbien.server.web.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 控制台相关配置
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "orbien.console")
public class ConsoleProperties {

    /**
     * 开发模式
     */
    private boolean dev = false;

    /**
     * 演示模式
     */
    private boolean previewMode = false;

    /**
     * 可覆盖后端对外 origin（拼 OAuth redirect_uri）
     * 默认同域从请求自动推导；仅在反向代理未正确传递 Forwarded 头时配置
     */
    private String publicUrl;

    /**
     * 可覆盖 OAuth 完成后的前端 origin
     * 生产同域无需配置；开发经 Vite 代理时可自动推导
     */
    private String frontendUrl;
}
