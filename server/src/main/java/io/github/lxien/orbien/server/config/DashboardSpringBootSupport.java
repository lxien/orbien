package io.github.lxien.orbien.server.config;

import io.github.lxien.orbien.common.utils.StringUtils;
import io.github.lxien.orbien.server.config.domain.DashboardConfig;
import org.springframework.boot.builder.SpringApplicationBuilder;

import java.nio.file.Path;

/**
 * 将 {@link DashboardConfig} 映射为 Spring Boot 嵌入式 Web 容器属性。
 */
public final class DashboardSpringBootSupport {

    private DashboardSpringBootSupport() {
    }

    public static void apply(SpringApplicationBuilder builder, DashboardConfig dashboard) {
        builder.properties("spring.main.web-application-type=servlet");
        builder.properties("server.port=" + dashboard.getPort());
        if (StringUtils.hasText(dashboard.getAddr())) {
            builder.properties("server.address=" + dashboard.getAddr());
        }
        if (!dashboard.isSslEnabled()) {
            return;
        }
        builder.properties(
                "server.ssl.enabled=true",
                "server.ssl.certificate=" + toFileUri(dashboard.getCertFile()),
                "server.ssl.certificate-private-key=" + toFileUri(dashboard.getKeyFile())
        );
        if (StringUtils.hasText(dashboard.getKeyPassword())) {
            builder.properties("server.ssl.certificate-private-key-password=" + dashboard.getKeyPassword());
        }
    }

    private static String toFileUri(String path) {
        return Path.of(path).toAbsolutePath().toUri().toString();
    }
}
