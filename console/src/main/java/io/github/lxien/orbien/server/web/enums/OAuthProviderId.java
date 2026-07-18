package io.github.lxien.orbien.server.web.enums;

import io.github.lxien.orbien.server.web.common.exception.BizException;
import lombok.Getter;

@Getter
public enum OAuthProviderId {
    GITHUB("GitHub"),
    GITEE("Gitee"),
    GOOGLE("Google");

    private final String displayName;

    OAuthProviderId(String displayName) {
        this.displayName = displayName;
    }

    public static OAuthProviderId fromPath(String value) {
        if (value == null || value.isBlank()) {
            throw new BizException("未知 OAuth 平台");
        }
        try {
            return OAuthProviderId.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BizException("不支持的 OAuth 平台: " + value);
        }
    }
}
