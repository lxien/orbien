package io.github.lxien.orbien.server.web.oauth;

import io.github.lxien.orbien.server.web.enums.OAuthProviderId;

public record OAuthState(
        OAuthProviderId provider,
        OAuthPurpose purpose,
        String username,
        String frontendOrigin
) {
}
