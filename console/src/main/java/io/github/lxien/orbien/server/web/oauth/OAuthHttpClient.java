package io.github.lxien.orbien.server.web.oauth;

import io.github.lxien.orbien.server.web.common.exception.BizException;
import io.github.lxien.orbien.server.web.common.utils.JsonUtils;
import io.github.lxien.orbien.server.web.enums.OAuthProviderId;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class OAuthHttpClient {

    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    public String exchangeCode(OAuthProviderId provider,
                               String clientId,
                               String clientSecret,
                               String code,
                               String redirectUri) {
        OAuthProviderMeta.Meta meta = OAuthProviderMeta.of(provider);
        Map<String, String> form = new LinkedHashMap<>();
        form.put("client_id", clientId);
        form.put("client_secret", clientSecret);
        form.put("code", code);
        form.put("redirect_uri", redirectUri);
        form.put("grant_type", "authorization_code");

        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(meta.tokenUrl()))
                    .timeout(Duration.ofSeconds(15))
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(encodeForm(form)));

            HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new BizException("换取 access_token 失败: HTTP " + response.statusCode());
            }
            Map<String, Object> body = JsonUtils.toMap(response.body());
            Object accessToken = body.get("access_token");
            if (accessToken == null || !StringUtils.hasText(accessToken.toString())) {
                Object error = body.getOrDefault("error_description", body.get("error"));
                throw new BizException("换取 access_token 失败" + (error != null ? ": " + error : ""));
            }
            return accessToken.toString();
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new BizException("换取 access_token 异常: " + e.getMessage());
        }
    }

    public OAuthUserProfile fetchProfile(OAuthProviderId provider, String accessToken) {
        OAuthProviderMeta.Meta meta = OAuthProviderMeta.of(provider);
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(meta.userInfoUrl()))
                    .timeout(Duration.ofSeconds(15))
                    .header("Accept", "application/json")
                    .header("Authorization", "Bearer " + accessToken)
                    .header("User-Agent", "Orbien-Console")
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new BizException("获取用户信息失败: HTTP " + response.statusCode());
            }
            Map<String, Object> body = JsonUtils.toMap(response.body());
            return parseProfile(provider, body);
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new BizException("获取用户信息异常: " + e.getMessage());
        }
    }

    private static OAuthUserProfile parseProfile(OAuthProviderId provider, Map<String, Object> body) {
        return switch (provider) {
            case GITHUB, GITEE -> {
                Object id = body.get("id");
                Object login = body.get("login");
                if (id == null) {
                    throw new BizException("平台未返回用户 ID");
                }
                String externalLogin = login != null ? login.toString() : id.toString();
                yield new OAuthUserProfile(id.toString(), externalLogin);
            }
            case GOOGLE -> {
                Object sub = body.get("sub");
                if (sub == null) {
                    throw new BizException("平台未返回用户 ID");
                }
                Object email = body.get("email");
                Object name = body.get("name");
                String externalLogin = email != null ? email.toString()
                        : (name != null ? name.toString() : sub.toString());
                yield new OAuthUserProfile(sub.toString(), externalLogin);
            }
        };
    }

    private static String encodeForm(Map<String, String> form) {
        return form.entrySet().stream()
                .map(e -> URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8)
                        + "="
                        + URLEncoder.encode(e.getValue() == null ? "" : e.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));
    }
}
