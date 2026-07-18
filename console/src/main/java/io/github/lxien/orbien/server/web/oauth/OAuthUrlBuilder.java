package io.github.lxien.orbien.server.web.oauth;

import io.github.lxien.orbien.server.web.config.ConsoleProperties;
import io.github.lxien.orbien.server.web.enums.OAuthProviderId;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Locale;

/**
 * OAuth 相关 URL 解析
 * <p>
 * 生产同域单体：默认从当前请求（含 Forwarded）推导，无需额外配置
 * 开发前后端分离：经 Vite 代理并开启 xfwd 后，与同域一致；亦可由前端传递经校验的 return_origin
 */
@Component
@RequiredArgsConstructor
public class OAuthUrlBuilder {

    private final ConsoleProperties consoleProperties;

    /**
     * 后端对外 origin，用于拼 OAuth redirect_uri
     */
    public String publicOrigin(HttpServletRequest request) {
        if (StringUtils.hasText(consoleProperties.getPublicUrl())) {
            return trimTrailingSlash(consoleProperties.getPublicUrl().trim());
        }
        return requestOrigin(request);
    }

    /**
     * OAuth 完成后跳回前端的 origin
     *
     * @param requestedReturnOrigin 前端显式声明的 origin，可为 null
     */
    public String resolveFrontendOrigin(HttpServletRequest request, String requestedReturnOrigin) {
        if (StringUtils.hasText(consoleProperties.getFrontendUrl())) {
            return trimTrailingSlash(consoleProperties.getFrontendUrl().trim());
        }

        String publicOrigin = publicOrigin(request);
        String fromHeaders = originFromHeaders(request);
        String requested = normalizeHttpOrigin(requestedReturnOrigin);
        if (requested != null && isAllowedReturnOrigin(requested, publicOrigin, fromHeaders)) {
            return requested;
        }
        if (StringUtils.hasText(fromHeaders)) {
            return fromHeaders;
        }
        return publicOrigin;
    }

    public String resolveFrontendOrigin(HttpServletRequest request) {
        return resolveFrontendOrigin(request, null);
    }

    public String callbackUrl(HttpServletRequest request, OAuthProviderId provider) {
        return publicOrigin(request) + "/api/auth/oauth/callback/" + provider.name().toLowerCase(Locale.ROOT);
    }

    public String frontendLoginError(String frontendOrigin, String error) {
        return trimTrailingSlash(frontendOrigin) + "/#/auth/login?oauthError=" + error;
    }

    public String frontendTicketCallback(String frontendOrigin, String ticket) {
        return trimTrailingSlash(frontendOrigin) + "/#/auth/oauth/callback?ticket=" + ticket;
    }

    public String frontendBindResult(String frontendOrigin, String result) {
        return trimTrailingSlash(frontendOrigin) + "/#/system/user-center?bind=" + result;
    }

    public String buildAuthorizeUrl(OAuthProviderId provider,
                                    String clientId,
                                    String redirectUri,
                                    String state) {
        OAuthProviderMeta.Meta meta = OAuthProviderMeta.of(provider);
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(meta.authorizeUrl())
                .queryParam("client_id", clientId)
                .queryParam("redirect_uri", redirectUri)
                .queryParam("response_type", "code")
                .queryParam("state", state)
                .queryParam("scope", String.join(" ", meta.scopes()));
        if (provider == OAuthProviderId.GOOGLE) {
            builder.queryParam("access_type", "online");
            builder.queryParam("prompt", "select_account");
        }
        return builder.build().encode().toUriString();
    }

    private boolean isAllowedReturnOrigin(String candidate, String publicOrigin, String fromHeaders) {
        return equalsOrigin(candidate, publicOrigin)
                || equalsOrigin(candidate, fromHeaders)
                || equalsOrigin(candidate, normalizeHttpOrigin(consoleProperties.getFrontendUrl()));
    }

    private static boolean equalsOrigin(String a, String b) {
        return StringUtils.hasText(a) && StringUtils.hasText(b) && a.equalsIgnoreCase(b);
    }

    private String requestOrigin(HttpServletRequest request) {
        String scheme = request.getScheme();
        String host = request.getServerName();
        int port = request.getServerPort();
        boolean defaultPort = ("http".equals(scheme) && port == 80)
                || ("https".equals(scheme) && port == 443);
        if (defaultPort) {
            return scheme + "://" + host;
        }
        return scheme + "://" + host + ":" + port;
    }

    private String originFromHeaders(HttpServletRequest request) {
        String origin = request.getHeader("Origin");
        String normalized = normalizeHttpOrigin(origin);
        if (normalized != null) {
            return normalized;
        }
        return normalizeHttpOrigin(refererOrigin(request.getHeader("Referer")));
    }

    private static String refererOrigin(String referer) {
        if (!StringUtils.hasText(referer)) {
            return null;
        }
        try {
            URI uri = URI.create(referer.trim());
            if (uri.getScheme() == null || uri.getHost() == null) {
                return null;
            }
            int port = uri.getPort();
            if (port > 0) {
                return uri.getScheme() + "://" + uri.getHost() + ":" + port;
            }
            return uri.getScheme() + "://" + uri.getHost();
        } catch (Exception ignored) {
            return null;
        }
    }

    /**
     * 仅接受 http(s) 的 origin（scheme://host[:port]），拒绝带 path 的开放重定向
     */
    static String normalizeHttpOrigin(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String trimmed = value.trim();
        try {
            URI uri = URI.create(trimmed);
            String scheme = uri.getScheme();
            if (scheme == null || uri.getHost() == null) {
                return null;
            }
            scheme = scheme.toLowerCase(Locale.ROOT);
            if (!"http".equals(scheme) && !"https".equals(scheme)) {
                return null;
            }
            if (StringUtils.hasText(uri.getRawPath()) && !"/".equals(uri.getRawPath())) {
                return null;
            }
            if (StringUtils.hasText(uri.getRawQuery()) || StringUtils.hasText(uri.getRawFragment())) {
                return null;
            }
            int port = uri.getPort();
            boolean defaultPort = ("http".equals(scheme) && port == 80)
                    || ("https".equals(scheme) && port == 443)
                    || port < 0;
            if (defaultPort) {
                return scheme + "://" + uri.getHost();
            }
            return scheme + "://" + uri.getHost() + ":" + port;
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String trimTrailingSlash(String url) {
        if (url.endsWith("/")) {
            return url.substring(0, url.length() - 1);
        }
        return url;
    }
}
