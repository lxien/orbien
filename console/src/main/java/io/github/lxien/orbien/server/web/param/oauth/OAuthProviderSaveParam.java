package io.github.lxien.orbien.server.web.param.oauth;

import lombok.Data;

/**
 * OAuth Provider 凭证配置
 */
@Data
public class OAuthProviderSaveParam {
    private String clientId;
    private String clientSecret;
}
