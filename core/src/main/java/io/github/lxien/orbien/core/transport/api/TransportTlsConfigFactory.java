package io.github.lxien.orbien.core.transport.api;

import io.github.lxien.orbien.core.domain.TlsConfig;
import io.github.lxien.orbien.core.enums.TransportProtocol;

/**
 * 按 本次建连是否加密生成 Pipeline 使用的 TLS 配置副本，避免修改全局配置对象。
 */
public final class TransportTlsConfigFactory {

    private TransportTlsConfigFactory() {
    }

    /**
     * 控制通道：沿用全局 TLS 开关。
     */
    public static TlsConfig forControlChannel(TlsConfig global) {
        if (global == null) {
            return new TlsConfig(false);
        }
        return copy(global);
    }

    /**
     * TCP 按 {@code connectionEncrypt} 决定是否启用 TLS；WS/QUIC 始终启用。
     */
    public static TlsConfig forDataTunnel(TlsConfig global, TransportProtocol protocol, boolean connectionEncrypt) {
        if (TransportEncryptResolver.requiresTls(protocol)) {
            return global != null ? copy(global) : new TlsConfig(true);
        }
        TlsConfig tls = global != null ? copy(global) : new TlsConfig(false);
        tls.setEnabled(global != null && global.isEnabled() && connectionEncrypt);
        return tls;
    }

    private static TlsConfig copy(TlsConfig source) {
        TlsConfig tls = new TlsConfig(source.isEnabled());
        tls.setCertFile(source.getCertFile());
        tls.setKeyFile(source.getKeyFile());
        tls.setCaFile(source.getCaFile());
        tls.setKeyPassword(source.getKeyPassword());
        return tls;
    }
}
