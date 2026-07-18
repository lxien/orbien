package io.github.lxien.orbien.server.web.service;

import io.github.lxien.orbien.server.web.dto.oauth.OAuthProviderConfigDTO;
import io.github.lxien.orbien.server.web.dto.oauth.OAuthPublicProviderDTO;
import io.github.lxien.orbien.server.web.enums.OAuthProviderId;
import io.github.lxien.orbien.server.web.param.oauth.OAuthProviderEnableParam;
import io.github.lxien.orbien.server.web.param.oauth.OAuthProviderSaveParam;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

public interface OAuthProviderConfigService {

    List<OAuthPublicProviderDTO> listEnabledPublic();

    List<OAuthProviderConfigDTO> listAll(HttpServletRequest request);

    /**
     * 保存 Provider 凭证配置（Client ID / Secret）
     */
    OAuthProviderConfigDTO save(OAuthProviderId provider, OAuthProviderSaveParam param, HttpServletRequest request);

    /**
     * 更新 Provider 登录启用状态
     */
    OAuthProviderConfigDTO updateEnabled(OAuthProviderId provider, OAuthProviderEnableParam param, HttpServletRequest request);

    record ResolvedCredentials(String clientId, String clientSecret) {
    }

    /**
     * 要求已配置 Client ID 与 Client Secret
     */
    ResolvedCredentials requireConfiguredCredentials(OAuthProviderId provider);

    /**
     * 要求已配置且已启用
     */
    ResolvedCredentials requireEnabledCredentials(OAuthProviderId provider);
}
