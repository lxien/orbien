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
}
