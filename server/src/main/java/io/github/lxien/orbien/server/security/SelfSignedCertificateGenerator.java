package io.github.lxien.orbien.server.security;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.*;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.X509Certificate;
import java.util.Date;

/**
 * 自签名SSL证书生成器
 */
public final class SelfSignedCertificateGenerator {

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    private SelfSignedCertificateGenerator() {
    }

    public record Result(PrivateKey privateKey, X509Certificate certificate) {
    }

    /**
     * 生成自签名SSL证书。
     * <p>
     * 证书配置：
     * <ul>
     * <li>密钥对：EC secp256r1 椭圆曲线密钥对</li>
     * <li>签名：SHA256withECDSA 数字签名</li>
     * <li>有效期：3650天（10年）</li>
     * </ul>
     *
     * @return 私钥和证书
     */
    public static Result generate() throws Exception {
        SecureRandom random = new SecureRandom();
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC", "BC");
        kpg.initialize(org.bouncycastle.jce.ECNamedCurveTable.getParameterSpec("secp256r1"), random);
        KeyPair keyPair = kpg.generateKeyPair();

        X500Name subject = new X500Name("CN=localhost");
        Date now = new Date();

        JcaX509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(
                subject,
                BigInteger.valueOf(now.getTime()),
                new Date(now.getTime() - 60000),
                new Date(now.getTime() + 3650L * 86400000),
                subject,
                keyPair.getPublic()
        );

        builder.addExtension(org.bouncycastle.asn1.x509.Extension.keyUsage, true,
                new KeyUsage(KeyUsage.digitalSignature | KeyUsage.keyEncipherment));
        builder.addExtension(org.bouncycastle.asn1.x509.Extension.extendedKeyUsage, false,
                new ExtendedKeyUsage(KeyPurposeId.id_kp_serverAuth));
        builder.addExtension(org.bouncycastle.asn1.x509.Extension.subjectAlternativeName, false,
                new GeneralNames(new GeneralName(GeneralName.dNSName, "localhost")));

        var signer = new JcaContentSignerBuilder("SHA256withECDSA").setProvider("BC").build(keyPair.getPrivate());
        return new Result(keyPair.getPrivate(),
                new JcaX509CertificateConverter().setProvider("BC").getCertificate(builder.build(signer)));
    }

    public static void writeToPemFiles(Result result, File privateKey, File certificate) throws IOException {
        try (Writer w = new FileWriter(privateKey);
             JcaPEMWriter pemWriter = new JcaPEMWriter(w)) {
            pemWriter.writeObject(result.privateKey());
        }
        try (Writer w = new FileWriter(certificate);
             JcaPEMWriter pemWriter = new JcaPEMWriter(w)) {
            pemWriter.writeObject(result.certificate());
        }
    }
}
