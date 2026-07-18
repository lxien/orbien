package io.github.lxien.orbien.server.web.oauth;

import io.github.lxien.orbien.server.web.common.exception.BizException;
import io.github.lxien.orbien.server.web.common.utils.JsonUtils;
import io.github.lxien.orbien.server.web.dto.auth.LoginDTO;
import io.github.lxien.orbien.server.web.enums.OAuthProviderId;
import io.github.lxien.orbien.server.web.service.AuthService;
import io.github.lxien.orbien.server.web.service.OAuthBindingService;
import io.github.lxien.orbien.server.web.service.OAuthProviderConfigService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
public class OAuthFacade {

    private final OAuthProviderConfigService oauthProviderConfigService;
    private final OAuthBindingService oauthBindingService;
    private final OAuthStateStore oauthStateStore;
    private final OAuthTicketStore oauthTicketStore;
    private final OAuthHttpClient oauthHttpClient;
    private final OAuthUrlBuilder oauthUrlBuilder;
    private final AuthService authService;

    public String startAuthorize(OAuthProviderId provider,
                                 OAuthPurpose purpose,
                                 String username,
                                 String returnOrigin,
                                 HttpServletRequest request) {
        OAuthProviderConfigService.ResolvedCredentials credentials = purpose == OAuthPurpose.LOGIN
                ? oauthProviderConfigService.requireEnabledCredentials(provider)
                : oauthProviderConfigService.requireConfiguredCredentials(provider);
        String frontendOrigin = oauthUrlBuilder.resolveFrontendOrigin(request, returnOrigin);
        String state = oauthStateStore.put(new OAuthState(provider, purpose, username, frontendOrigin));
        String redirectUri = oauthUrlBuilder.callbackUrl(request, provider);
        return oauthUrlBuilder.buildAuthorizeUrl(provider, credentials.clientId(), redirectUri, state);
    }

    public void handleCallback(OAuthProviderId provider,
                               String code,
                               String state,
                               String error,
                               HttpServletRequest request,
                               HttpServletResponse response) throws IOException {
        String frontendOrigin = oauthUrlBuilder.resolveFrontendOrigin(request);
        if (StringUtils.hasText(error) || !StringUtils.hasText(code)) {
            writeHtmlRedirect(response, oauthUrlBuilder.frontendLoginError(frontendOrigin, "denied"));
            return;
        }

        OAuthState oauthState;
        try {
            oauthState = oauthStateStore.consume(state);
        } catch (BizException e) {
            writeHtmlRedirect(response, oauthUrlBuilder.frontendLoginError(frontendOrigin, "invalid_state"));
            return;
        }
        if (oauthState.provider() != provider) {
            writeHtmlRedirect(response, oauthUrlBuilder.frontendLoginError(frontendOrigin, "invalid_state"));
            return;
        }
        final String spaOrigin = StringUtils.hasText(oauthState.frontendOrigin())
                ? oauthState.frontendOrigin()
                : frontendOrigin;

        try {
            OAuthProviderConfigService.ResolvedCredentials credentials =
                    oauthState.purpose() == OAuthPurpose.LOGIN
                            ? oauthProviderConfigService.requireEnabledCredentials(provider)
                            : oauthProviderConfigService.requireConfiguredCredentials(provider);
            String redirectUri = oauthUrlBuilder.callbackUrl(request, provider);
            String accessToken = oauthHttpClient.exchangeCode(
                    provider, credentials.clientId(), credentials.clientSecret(), code, redirectUri);
            OAuthUserProfile profile = oauthHttpClient.fetchProfile(provider, accessToken);

            if (oauthState.purpose() == OAuthPurpose.BIND) {
                if (!StringUtils.hasText(oauthState.username())) {
                    writeHtmlRedirect(response, oauthUrlBuilder.frontendBindResult(spaOrigin, "fail"));
                    return;
                }
                oauthBindingService.bind(oauthState.username(), provider, profile);
                writeHtmlRedirect(response, oauthUrlBuilder.frontendBindResult(spaOrigin, "ok"));
                return;
            }

            String target = oauthBindingService.findUsername(provider, profile.externalId())
                    .map(username -> {
                        String ticket = oauthTicketStore.put(username);
                        return oauthUrlBuilder.frontendTicketCallback(spaOrigin, ticket);
                    })
                    .orElseGet(() -> oauthUrlBuilder.frontendLoginError(spaOrigin, "user_not_found"));
            writeHtmlRedirect(response, target);
        } catch (BizException e) {
            if (oauthState.purpose() == OAuthPurpose.BIND) {
                writeHtmlRedirect(response, oauthUrlBuilder.frontendBindResult(spaOrigin, "fail"));
            } else {
                writeHtmlRedirect(response, oauthUrlBuilder.frontendLoginError(spaOrigin, "failed"));
            }
        }
    }

    public LoginDTO exchangeTicket(String ticket) {
        String username = oauthTicketStore.consume(ticket);
        return authService.issueToken(username);
    }

    /**
     * 用 HTML + JS 跳转，避免 302 Location 丢弃 hash，以及开发态后端无 SPA 时落到 `/` 报错
     */
    public static void writeHtmlRedirect(HttpServletResponse response, String targetUrl) throws IOException {
        String safeJson = JsonUtils.toJson(targetUrl);
        String html = """
                <!DOCTYPE html>
                <html lang="zh-CN">
                <head>
                  <meta charset="UTF-8"/>
                  <meta http-equiv="refresh" content="0;url=__URL_ATTR__"/>
                  <title>Redirecting…</title>
                  <script>location.replace(__URL_JS__);</script>
                </head>
                <body>正在返回控制台…</body>
                </html>
                """
                .replace("__URL_ATTR__", htmlEscapeAttribute(targetUrl))
                .replace("__URL_JS__", safeJson);
        response.setStatus(HttpServletResponse.SC_OK);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("text/html;charset=UTF-8");
        response.getWriter().write(html);
        response.getWriter().flush();
    }

    private static String htmlEscapeAttribute(String value) {
        return value
                .replace("&", "&amp;")
                .replace("\"", "&quot;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}
