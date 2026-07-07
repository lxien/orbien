package io.github.lxien.orbien.server.config.domain;

import io.github.lxien.orbien.common.utils.StringUtils;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class DashboardConfig {
    private Boolean enabled;
    private String username;
    private String password;
    private String addr;
    private Integer port;
    private String certFile;
    private String keyFile;
    private String keyPassword;

    public DashboardConfig(boolean enabled) {
        this.enabled=enabled;
    }
    public DashboardConfig(Boolean enabled, String username, String password, String addr, Integer port) {
        this.enabled = enabled;
        this.username = username;
        this.password = password;
        this.addr = addr;
        this.port = port;
    }

    public boolean isTlsEnabled() {
        return StringUtils.hasText(certFile) && StringUtils.hasText(keyFile);
    }
}
