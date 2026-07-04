package io.github.lxien.orbien.server.config.domain;

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
}
