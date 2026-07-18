package io.github.lxien.orbien.server.web.oauth;

public record OAuthUserProfile(
        String externalId,
        String externalLogin
) {
}
