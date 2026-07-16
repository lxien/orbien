package io.github.lxien.orbien.client.config.domain;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LogConfig {
    private String path = "log";
    private String name = "orbien.log";
    private String archivePattern = "orbien.%d{yyyy-MM-dd}.log";
    private String logPattern = "%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n";
    private int maxHistory = 30;
    private String totalSizeCap = "3GB";
    private String level = "info";
}
