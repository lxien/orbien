package io.github.lxien.orbien.server.web.oauth;

import io.github.lxien.orbien.server.web.enums.OAuthProviderId;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * OAuth 平台固定元数据
 */
public final class OAuthProviderMeta {

    public record Meta(
            String authorizeUrl,
            String tokenUrl,
            String userInfoUrl,
            List<String> scopes
    ) {
    }

    private static final Map<OAuthProviderId, Meta> METAS = new EnumMap<>(OAuthProviderId.class);

    static {
        METAS.put(OAuthProviderId.GITHUB, new Meta(
                "https://github.com/login/oauth/authorize",
                "https://github.com/login/oauth/access_token",
                "https://api.github.com/user",
                List.of("read:user", "user:email")
        ));
        METAS.put(OAuthProviderId.GITEE, new Meta(
                "https://gitee.com/oauth/authorize",
                "https://gitee.com/oauth/token",
                "https://gitee.com/api/v5/user",
                List.of("user_info")
        ));
        METAS.put(OAuthProviderId.GOOGLE, new Meta(
                "https://accounts.google.com/o/oauth2/v2/auth",
                "https://oauth2.googleapis.com/token",
                "https://openidconnect.googleapis.com/v1/userinfo",
                List.of("openid", "email", "profile")
        ));
    }

    private OAuthProviderMeta() {
    }

    public static Meta of(OAuthProviderId provider) {
        Meta meta = METAS.get(provider);
        if (meta == null) {
            throw new IllegalArgumentException("未配置 OAuth 元数据: " + provider);
        }
        return meta;
    }
}
