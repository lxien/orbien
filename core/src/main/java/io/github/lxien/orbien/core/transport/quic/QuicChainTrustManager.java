package io.github.lxien.orbien.core.transport.quic;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509ExtendedTrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.File;
import java.io.FileInputStream;
import java.net.Socket;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Collection;

/**
 * QUIC(BoringSSL) 校验证书链时不做 hostname 校验，并兼容 TLS1.3 的 authType 差异。
 * Netty QUIC 对 {@code localhost} / IP 地址不发送 SNI；BoringSSL 在 TLS1.3 下传递
 * {@code GENERIC} 作为 authType，而 JDK TrustManager 期望 {@code UNKNOWN}。
 */
final class QuicChainTrustManager {

    private QuicChainTrustManager() {
    }

    static TrustManager create(File caFile) throws Exception {
        TrustManagerFactory tmf = buildTrustManagerFactory(caFile);
        X509TrustManager delegate = (X509TrustManager) tmf.getTrustManagers()[0];
        return new X509ExtendedTrustManager() {
            @Override
            public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                delegate.checkClientTrusted(chain, normalizeAuthType(authType));
            }

            @Override
            public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                delegate.checkServerTrusted(chain, normalizeAuthType(authType));
            }

            @Override
            public void checkClientTrusted(X509Certificate[] chain, String authType, Socket socket)
                    throws CertificateException {
                delegate.checkClientTrusted(chain, normalizeAuthType(authType));
            }

            @Override
            public void checkServerTrusted(X509Certificate[] chain, String authType, Socket socket)
                    throws CertificateException {
                delegate.checkServerTrusted(chain, normalizeAuthType(authType));
            }

            @Override
            public void checkClientTrusted(X509Certificate[] chain, String authType, SSLEngine engine)
                    throws CertificateException {
                delegate.checkClientTrusted(chain, normalizeAuthType(authType));
            }

            @Override
            public void checkServerTrusted(X509Certificate[] chain, String authType, SSLEngine engine)
                    throws CertificateException {
                delegate.checkServerTrusted(chain, normalizeAuthType(authType));
            }

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return delegate.getAcceptedIssuers();
            }
        };
    }

    /**
     * BoringSSL 在 TLS1.3 握手时传入 GENERIC，JDK 默认 TrustManager 无法识别。
     * netty-incubator-codec-quic 0.0.74+ 已在框架层修复，此处保留兼容旧版本及双保险。
     */
    private static String normalizeAuthType(String authType) {
        if (authType == null || authType.isEmpty()) {
            return "UNKNOWN";
        }
        if ("GENERIC".equalsIgnoreCase(authType)) {
            return "UNKNOWN";
        }
        return authType;
    }

    private static TrustManagerFactory buildTrustManagerFactory(File caFile) throws Exception {
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        Collection<? extends Certificate> certs;
        try (FileInputStream in = new FileInputStream(caFile)) {
            certs = cf.generateCertificates(in);
        }
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        keyStore.load(null);
        int index = 0;
        for (Certificate cert : certs) {
            keyStore.setCertificateEntry("ca-" + index++, cert);
        }
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(keyStore);
        return tmf;
    }
}
