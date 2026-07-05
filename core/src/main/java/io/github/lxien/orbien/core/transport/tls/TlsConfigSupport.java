package io.github.lxien.orbien.core.transport.tls;

import io.github.lxien.orbien.common.utils.StringUtils;
import io.github.lxien.orbien.core.domain.TlsConfig;

import java.nio.file.Path;
import java.nio.file.Paths;

public final class TlsConfigSupport {

    private TlsConfigSupport() {
    }

    public static boolean hasClientCredentials(TlsConfig tlsConfig) {
        return tlsConfig != null
                && StringUtils.hasText(tlsConfig.getCertFile())
                && StringUtils.hasText(tlsConfig.getKeyFile());
    }

    /**
     * 将 TLS 证书路径解析为相对配置文件目录的绝对路径。
     */
    public static TlsConfig resolveAbsolutePaths(TlsConfig tlsConfig, Path configDir) {
        if (tlsConfig == null || configDir == null) {
            return tlsConfig;
        }
        return new TlsConfig(
                tlsConfig.isEnabled(),
                resolvePath(configDir, tlsConfig.getCertFile()),
                resolvePath(configDir, tlsConfig.getKeyFile()),
                resolvePath(configDir, tlsConfig.getCaFile()),
                tlsConfig.getKeyPassword()
        );
    }

    private static String resolvePath(Path configDir, String path) {
        if (!StringUtils.hasText(path)) {
            return path;
        }
        Path resolved = Paths.get(path);
        if (resolved.isAbsolute()) {
            return path;
        }
        return configDir.resolve(resolved).normalize().toString();
    }

    /**
     * 合并 TLS 配置：优先保留 target 的 enabled/port 等，缺失的证书字段从 fallback 补齐。
     */
    public static TlsConfig merge(TlsConfig fallback, TlsConfig target) {
        if (fallback == null) {
            return target;
        }
        if (target == null) {
            return fallback;
        }
        TlsConfig merged = new TlsConfig(
                target.isEnabled(),
                pick(target.getCertFile(), fallback.getCertFile()),
                pick(target.getKeyFile(), fallback.getKeyFile()),
                pick(target.getCaFile(), fallback.getCaFile()),
                pick(target.getKeyPassword(), fallback.getKeyPassword())
        );
        return merged;
    }

    public static TlsConfig effective(TlsConfig primary, TlsConfig fallback) {
        if (primary == null) {
            return fallback;
        }
        if (fallback == null || hasClientCredentials(primary)) {
            return primary;
        }
        return merge(fallback, primary);
    }

    private static String pick(String preferred, String alternate) {
        return StringUtils.hasText(preferred) ? preferred : alternate;
    }
}
