package io.github.lxien.orbien.core.transport.tls;

import io.github.lxien.orbien.core.utils.StringUtils;
import io.github.lxien.orbien.core.domain.TlsConfig;
import io.netty.handler.ssl.*;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.handler.ssl.util.SelfSignedCertificate;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.cert.CertificateException;
import java.util.List;

public class TlsHelper {

    private static final String TLS_V1_3 = "TLSv1.3";
    private static final String TLS_V1_2 = "TLSv1.2";

    public static SslContext buildSslContext(boolean forClient, TlsConfig tlsConfig, boolean isTestMode) throws IOException, CertificateException {
        SslProvider provider = SslProvider.JDK;
        if (forClient) {
            return buildClientSslContext(tlsConfig, isTestMode, provider);
        } else {
            return buildServerSslContext(tlsConfig, isTestMode, provider);
        }
    }

    private static SslContext buildClientSslContext(TlsConfig tlsConfig, boolean isTestMode, SslProvider provider) throws IOException {
        if (isTestMode) {
            return createTestSslContextForClient(provider);
        } else {
            return createSslContextForClient(tlsConfig, provider);
        }
    }


    private static SslContext buildServerSslContext(TlsConfig tlsConfig, boolean isTestMode, SslProvider provider) throws IOException, CertificateException {
        if (isTestMode) {
            return createTestSslContextForServer(provider);
        } else {
            return createSslContextForServer(tlsConfig, provider);
        }
    }

    private static SslContext createTestSslContextForClient(SslProvider provider) throws IOException {
        return SslContextBuilder
                .forClient()
                .protocols(getSupportedProtocols())
                .sslProvider(provider)
                .trustManager(InsecureTrustManagerFactory.INSTANCE)
                .build();
    }


    private static SslContext createSslContextForClient(TlsConfig tlsConfig, SslProvider provider) throws IOException {
        SslContextBuilder sslContextBuilder = SslContextBuilder
                .forClient()
                .protocols(getSupportedProtocols())
                .sslProvider(provider);

        if (!tlsConfig.mTLSEnabled()) {
            sslContextBuilder.trustManager(InsecureTrustManagerFactory.INSTANCE);
        } else {
            if (StringUtils.hasText(tlsConfig.getCaFile())) {
                sslContextBuilder.trustManager(new File(tlsConfig.getCaFile()));
            }
        }

        if (tlsConfig.mTLSEnabled() && !StringUtils.hasText(tlsConfig.getCertFile())) {
            throw new IllegalStateException("双向 TLS 已启用（配置了 ca_file），必须同时配置 cert_file 和 key_file");
        }

        if (StringUtils.hasText(tlsConfig.getCertFile()) && StringUtils.hasText(tlsConfig.getKeyFile())) {
            sslContextBuilder.keyManager(new File(tlsConfig.getCertFile()), new File(tlsConfig.getKeyFile()));
        }

        return sslContextBuilder.build();
    }


    private static SslContext createTestSslContextForServer(SslProvider provider) throws IOException, CertificateException {
        SelfSignedCertificate selfSignedCertificate = new SelfSignedCertificate();
        return SslContextBuilder
                .forServer(selfSignedCertificate.certificate(), selfSignedCertificate.privateKey())
                .sslProvider(provider)
                .protocols(getSupportedProtocols())
                .clientAuth(ClientAuth.OPTIONAL)
                .build();
    }

    private static SslContext createSslContextForServer(TlsConfig tlsConfig, SslProvider provider) throws IOException {
        SslContextBuilder sslContextBuilder = SslContextBuilder
                .forServer(
                        StringUtils.hasText(tlsConfig.getCertFile()) ? new FileInputStream(tlsConfig.getCertFile()) : null,
                        StringUtils.hasText(tlsConfig.getKeyFile()) ? new FileInputStream(tlsConfig.getKeyFile()) : null
                )
                .protocols(getSupportedProtocols())
                .sslProvider(provider);

        if (!tlsConfig.mTLSEnabled()) {
            sslContextBuilder.trustManager(InsecureTrustManagerFactory.INSTANCE);
        } else {
            if (StringUtils.hasText(tlsConfig.getCaFile())) {
                sslContextBuilder.trustManager(new File(tlsConfig.getCaFile()));
            }
        }
        sslContextBuilder.clientAuth(tlsConfig.getClientAuthMode());
        return sslContextBuilder.build();
    }

    /**
     * 返回支持的 TLS 协议列表，按优先级排序：JDK 11+ 优先 TLS 1.3，不支持时降级 TLS 1.2。
     */
    public static Iterable<String> getSupportedProtocols() {
        if (isJava11OrHigher()) {
            return List.of(TLS_V1_3, TLS_V1_2);
        }
        return List.of(TLS_V1_2);
    }

    public static boolean isJava11OrHigher() {
        String version = System.getProperty("java.specification.version");
        if (version == null || version.isEmpty()) {
            return false;
        }
        try {
            if (version.startsWith("1.")) {
                int minor = Integer.parseInt(version.substring(2));
                return minor >= 11;
            }
            int major = Integer.parseInt(version);
            return major >= 11;

        } catch (NumberFormatException e) {
            return false;
        }
    }
}