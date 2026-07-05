package io.github.lxien.orbien.core.transport.quic;

import io.github.lxien.orbien.core.domain.TlsConfig;
import io.github.lxien.orbien.core.transport.tls.TlsConfigSupport;
import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.incubator.codec.quic.QuicSslContext;
import io.netty.incubator.codec.quic.QuicSslContextBuilder;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.io.File;

final class QuicSslSupport {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(QuicSslSupport.class);

    /**
     * QUIC TLS 握手必须协商的 ALPN 协议名
     */
    static final String ALPN_PROTOCOL = "orbien";

    private QuicSslSupport() {
    }

    static QuicSslContext buildServerContext(TlsConfig tlsConfig) throws Exception {
        if (tlsConfig == null || tlsConfig.getKeyFile() == null || tlsConfig.getCertFile() == null) {
            throw new IllegalStateException("QUIC 服务端必须配置 cert_file 和 key_file");
        }
        File key = new File(tlsConfig.getKeyFile());
        File cert = new File(tlsConfig.getCertFile());
        logger.debug("[传输] QUIC 服务端 TLS key={} cert={}", key.getAbsolutePath(), cert.getAbsolutePath());
        QuicSslContextBuilder builder = QuicSslContextBuilder.forServer(key, tlsConfig.getKeyPassword(), cert)
                .applicationProtocols(ALPN_PROTOCOL);
        if (tlsConfig.mTLSEnabled()) {
            builder.clientAuth(ClientAuth.REQUIRE);
            builder.trustManager(QuicChainTrustManager.create(new File(tlsConfig.getCaFile())));
        }
        return builder.build();
    }

    static QuicSslContext buildClientContext(TlsConfig tlsConfig) throws Exception {
        QuicSslContextBuilder builder = QuicSslContextBuilder.forClient()
                .applicationProtocols(ALPN_PROTOCOL);
        if (TlsConfigSupport.hasClientCredentials(tlsConfig)) {
            builder.keyManager(
                    new File(tlsConfig.getKeyFile()),
                    tlsConfig.getKeyPassword(),
                    new File(tlsConfig.getCertFile()));
        }
        if (tlsConfig != null && tlsConfig.mTLSEnabled()) {
            File caFile = new File(tlsConfig.getCaFile());
            logger.debug("[传输] QUIC 客户端 TLS ca={} exists={}", caFile.getAbsolutePath(), caFile.exists());
            builder.trustManager(QuicChainTrustManager.create(caFile));
        } else {
            builder.trustManager(InsecureTrustManagerFactory.INSTANCE);
        }
        return builder.build();
    }
}
