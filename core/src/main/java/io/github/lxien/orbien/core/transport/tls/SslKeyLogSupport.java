package io.github.lxien.orbien.core.transport.tls;

import io.netty.incubator.codec.quic.BoringSSLKeylog;
import io.netty.incubator.codec.quic.QuicSslContextBuilder;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import javax.net.ssl.SSLEngine;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

/**
 * 支持通过 {@code SSLKEYLOGFILE} 环境变量导出 TLS 会话密钥，供 Wireshark 解密 QUIC 流量。
 * <p>
 * TCP / WebSocket 使用 JDK {@link io.netty.handler.ssl.SslProvider#JDK}，暂不支持此机制；
 */
public final class SslKeyLogSupport {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(SslKeyLogSupport.class);

    private SslKeyLogSupport() {
    }

    public static boolean isEnabled() {
        String path = System.getenv("SSLKEYLOGFILE");
        return path != null && !path.isBlank();
    }

    public static void applyQuicKeyLog(QuicSslContextBuilder builder) {
        BoringSSLKeylog keylog = createBoringSslKeylog();
        if (keylog != null) {
            builder.keylog(keylog);
            logger.debug("[传输] QUIC TLS 密钥日志已启用，写入文件: {}", System.getenv("SSLKEYLOGFILE"));
        }
    }

    static BoringSSLKeylog createBoringSslKeylog() {
        if (!isEnabled()) {
            return null;
        }
        Path keyLogPath = Paths.get(System.getenv("SSLKEYLOGFILE"));
        return new FileBoringSSLKeylog(keyLogPath);
    }

    private static final class FileBoringSSLKeylog implements BoringSSLKeylog {

        private final Path keyLogPath;
        private final Object lock = new Object();

        private FileBoringSSLKeylog(Path keyLogPath) {
            this.keyLogPath = keyLogPath;
        }

        @Override
        public void logKey(SSLEngine engine, String line) {
            synchronized (lock) {
                try {
                    Path parent = keyLogPath.getParent();
                    if (parent != null) {
                        Files.createDirectories(parent);
                    }
                    try (BufferedWriter writer = Files.newBufferedWriter(
                            keyLogPath,
                            StandardCharsets.US_ASCII,
                            StandardOpenOption.CREATE,
                            StandardOpenOption.APPEND)) {
                        writer.write(line);
                        writer.newLine();
                    }
                } catch (IOException e) {
                    logger.warn("[传输] 写入 TLS 密钥日志失败 path={}", keyLogPath, e);
                }
            }
        }
    }
}
