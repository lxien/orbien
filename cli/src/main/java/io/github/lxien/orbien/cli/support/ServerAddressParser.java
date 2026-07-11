package io.github.lxien.orbien.cli.support;

import io.github.lxien.orbien.credentials.CredentialsStore;
import io.github.lxien.orbien.core.utils.StringUtils;

public final class ServerAddressParser {

    private ServerAddressParser() {
    }

    public record ServerEndpoint(String host, int port) {
    }

    public static ServerEndpoint parse(String server, int defaultPort) {
        if (!StringUtils.hasText(server)) {
            throw new IllegalArgumentException("--server 不能为空");
        }
        String trimmed = server.trim();
        int port = defaultPort;
        String host;

        if (trimmed.startsWith("[")) {
            int closing = trimmed.indexOf(']');
            if (closing < 0) {
                throw new IllegalArgumentException("无效的服务端地址: " + server);
            }
            host = trimmed.substring(1, closing);
            String remainder = trimmed.substring(closing + 1);
            if (remainder.startsWith(":")) {
                port = parsePort(remainder.substring(1), server);
            }
        } else {
            int colon = trimmed.lastIndexOf(':');
            if (colon > 0 && trimmed.indexOf(':') == colon) {
                host = trimmed.substring(0, colon);
                port = parsePort(trimmed.substring(colon + 1), server);
            } else {
                host = trimmed;
            }
        }

        if (!StringUtils.hasText(host)) {
            throw new IllegalArgumentException("无效的服务端地址: " + server);
        }
        CredentialsStore.validatePort(port);
        return new ServerEndpoint(host, port);
    }

    private static int parsePort(String value, String original) {
        try {
            int port = Integer.parseInt(value.trim());
            CredentialsStore.validatePort(port);
            return port;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("无效的服务器地址: " + original);
        }
    }
}
