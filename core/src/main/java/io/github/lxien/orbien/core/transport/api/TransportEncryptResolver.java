package io.github.lxien.orbien.core.transport.api;

import io.github.lxien.orbien.core.enums.TransportProtocol;

/**
 * 解析数据隧道「是否走传输层 TLS」的最终生效值。
 * <p>
 * WebSocket / QUIC 由协议强制 TLS；TCP 在全局 TLS 可用时尊重代理级 {@code encrypt} 配置。
 */
public final class TransportEncryptResolver {

    private TransportEncryptResolver() {
    }

    /**
     * @param protocol         数据隧道协议
     * @param globalTlsEnabled 服务端/客户端全局 {@code transport.tls.enabled}
     * @param proxyEncrypt     代理级配置，{@code null} 视为开启（与 TOML 默认一致）
     */
    public static boolean resolveEffectiveEncrypt(TransportProtocol protocol, boolean globalTlsEnabled, Boolean proxyEncrypt) {
        if (protocol == null) {
            protocol = TransportProtocol.TCP;
        }
        return switch (protocol) {
            case WEBSOCKET, QUIC -> true;
            case TCP -> globalTlsEnabled && (proxyEncrypt == null || proxyEncrypt);
        };
    }

    public static boolean requiresTls(TransportProtocol protocol) {
        return protocol == TransportProtocol.WEBSOCKET || protocol == TransportProtocol.QUIC;
    }
}
