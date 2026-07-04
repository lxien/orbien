package io.github.lxien.orbien.core.transport;

import java.net.InetSocketAddress;
import java.util.Objects;

/**
 * UDP 会话索引键：监听端口 + 访问者地址
 */
public final class UdpSessionKey {
    private final int listenPort;
    private final String host;
    private final int port;

    public UdpSessionKey(int listenPort, String host, int port) {
        this.listenPort = listenPort;
        this.host = host;
        this.port = port;
    }

    public static UdpSessionKey of(int listenPort, InetSocketAddress address) {
        return new UdpSessionKey(listenPort, address.getAddress().getHostAddress(), address.getPort());
    }

    public int getListenPort() {
        return listenPort;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof UdpSessionKey that)) {
            return false;
        }
        return listenPort == that.listenPort && port == that.port && Objects.equals(host, that.host);
    }

    @Override
    public int hashCode() {
        return Objects.hash(listenPort, host, port);
    }
}
