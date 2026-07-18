package io.github.lxien.orbien.server.web.dto.oauth;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class OAuthBindingDTO {
    private String provider;
    private String displayName;
    private boolean bound;
    private String externalLogin;
    private LocalDateTime boundAt;
}
