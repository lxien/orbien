package io.github.lxien.orbien.server.web.param.oauth;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * OAuth Provider 启用状态
 */
@Data
public class OAuthProviderEnableParam {
    @NotNull
    private Boolean enabled;
}
