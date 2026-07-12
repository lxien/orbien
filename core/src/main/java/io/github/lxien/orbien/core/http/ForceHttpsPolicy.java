package io.github.lxien.orbien.core.http;

import io.github.lxien.orbien.core.domain.ProxyConfig;

/**
 * HTTPS 强制跳转策略：HTTPS 代理默认开启 HTTP→HTTPS 重定向，可通过 {@code force_https=false} 关闭。
 * 文件共享固定走 HTTPS，明文访问一律重定向。
 */
public final class ForceHttpsPolicy {

    public static final int DEFAULT_REDIRECT_STATUS = 308;

    private static final String ACME_CHALLENGE_PREFIX = "/.well-known/acme-challenge/";

    private ForceHttpsPolicy() {
    }

    /**
     * 是否应把明文 HTTP 请求重定向到 HTTPS。
     * 文件共享固定开启；HTTPS 代理在 {@code force_https} 未配置时默认 {@code true}。
     */
    public static boolean isRedirectEnabled(ProxyConfig config) {
        if (config == null) {
            return false;
        }
        if (config.isFile()) {
            return true;
        }
        if (!config.isHttps()) {
            return false;
        }
        Boolean forceHttps = config.getForceHttps();
        return forceHttps == null || forceHttps;
    }

    /**
     * 解析配置值；非 HTTPS/FILE 协议固定为 false
     */
    public static boolean resolveFlag(ProxyConfig config) {
        if (config == null || (!config.isHttps() && !config.isFile())) {
            return false;
        }
        return isRedirectEnabled(config);
    }

    public static boolean isAcmeChallengePath(String requestUri) {
        if (requestUri == null || requestUri.isEmpty()) {
            return false;
        }
        int queryIndex = requestUri.indexOf('?');
        String path = queryIndex >= 0 ? requestUri.substring(0, queryIndex) : requestUri;
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        return path.regionMatches(true, 0, ACME_CHALLENGE_PREFIX, 0, ACME_CHALLENGE_PREFIX.length());
    }

    /**
     * 构建重定向 Location，保留 path 与 query。
     */
    public static String buildHttpsLocation(String host, int httpsPort, String requestUri) {
        String normalizedHost = host == null ? "" : host.trim();
        String path = normalizeRequestUri(requestUri);
        String portPart = httpsPort == 443 ? "" : ":" + httpsPort;
        return "https://" + normalizedHost + portPart + path;
    }

    private static String normalizeRequestUri(String requestUri) {
        if (requestUri == null || requestUri.isEmpty()) {
            return "/";
        }
        if (requestUri.startsWith("/")) {
            return requestUri;
        }
        if (requestUri.startsWith("http://") || requestUri.startsWith("https://")) {
            int pathStart = requestUri.indexOf('/', requestUri.indexOf("://") + 3);
            return pathStart >= 0 ? requestUri.substring(pathStart) : "/";
        }
        return "/" + requestUri;
    }
}
