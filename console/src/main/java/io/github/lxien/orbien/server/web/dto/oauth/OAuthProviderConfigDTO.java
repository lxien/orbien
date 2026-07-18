package io.github.lxien.orbien.server.web.dto.oauth;

import lombok.Data;

@Data
public class OAuthProviderConfigDTO {
    private String provider;
    private String displayName;
    private boolean enabled;
    private String clientId;
    private boolean secretConfigured;
    private String callbackUrl;
}
