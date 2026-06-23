package com.xiaoniucode.etp.server.web.common.utils;

import lombok.Data;
import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMParser;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.security.MessageDigest;
import java.security.Security;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * SSL 证书解析器，用于从 PEM 格式的证书链中提取关键信息。
 *
 * <p>支持解析证书链并自动识别叶证书，提取通用名称、DNS SAN、颁发者信息、
 * 有效期及 SHA-256 指纹等属性。
 *
 * @author xiaoniucode
 * @since 1.0
 */
public class SslParser {

    static {
        // 注册 BouncyCastle 安全提供者
        try {
            if (Security.getProvider("BC") == null) Security.addProvider(new BouncyCastleProvider());
        } catch (Exception ignored) {
        }
    }

    /**
     * SSL 证书信息实体类。
     */
    @Data
    public static class SslInfo {

        /**
         * 通用名称列表
         */
        public List<String> commonNames = new ArrayList<>();

        /**
         * DNS 类型的主题备用名称列表
         */
        public List<String> dns = new ArrayList<>();

        /**
         * 颁发者
         */
        public String issuer;

        /**
         * 颁发者组织
         */
        public String organization;

        /**
         * 证书生效时间
         */
        public Date issuedAt;

        /**
         * 证书过期时间
         */
        public Date expiresAt;

        /**
         * SHA-256 指纹
         */
        public String sha256Fingerprint;

        /**
         * 扩展信息，用于存放解析过程中的诊断数据
         */
        public Map<String, Object> extra = new LinkedHashMap<>();

        public boolean hasError() {
            return extra.get("error") != null;
        }
    }

    /**
     * 解析 PEM 格式的证书链，提取 SSL 证书信息。
     *
     * @param fullChain PEM 格式的完整证书链
     * @return SSL 证书信息对象
     */
    public static SslInfo parsePem(String fullChain) {
        SslInfo info = new SslInfo();
        try {
            List<X509Certificate> certs = parseCertificates(fullChain, info);
            if (certs.isEmpty()) {
                info.extra.put("error", "no certificates parsed - fullchain must contain PEM CERTIFICATE blocks");
                return info;
            }
            X509Certificate leaf = selectLeafStrictWithFallback(certs, info);
            if (leaf == null) {
                info.extra.put("error", "cannot determine leaf certificate");
                return info;
            }
            fillFromCert(leaf, info);
        } catch (Exception e) {
            info.extra.put("error", e.getMessage());
        }
        return info;
    }

    /**
     * 从 PEM 格式字符串中解析证书列表。
     *
     * @param pem  PEM 格式字符串
     * @param info 用于存放解析错误信息的对象
     * @return 解析出的证书列表
     */
    private static List<X509Certificate> parseCertificates(String pem, SslInfo info) {
        List<X509Certificate> out = new ArrayList<>();
        if (pem == null) return out;
        try (Reader r = new StringReader(pem); PEMParser p = new PEMParser(r)) {
            Object o;
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            while ((o = p.readObject()) != null) {
                if (o instanceof X509CertificateHolder h) {
                    byte[] der = h.getEncoded();
                    X509Certificate cert = (X509Certificate) cf.generateCertificate(new java.io.ByteArrayInputStream(der));
                    out.add(cert);
                }
            }
        } catch (IOException | java.security.cert.CertificateException e) {
            info.extra.put("cert_parse_error", e.getMessage());
        }
        return out;
    }

    /**
     * 从证书链中选择叶证书（终端实体证书）。
     * <p>选择策略：
     * <ol>
     *   <li>优先选择未被其他证书签发的证书</li>
     *   <li>若存在多个候选，选择 BasicConstraints 为 -1 的终端实体证书</li>
     *   <li>若仍存在多个，选择 DNS SAN 数量最多的证书</li>
     *   <li>最后选择最早到期的证书</li>
     * </ol>
     *
     * @param certs 证书列表
     * @param info  用于存放诊断信息的对象
     * @return 叶证书，若无法确定则返回 null
     */
    private static X509Certificate selectLeafStrictWithFallback(List<X509Certificate> certs, SslInfo info) {
        // 第一阶段：找出未被其他证书签发的候选证书
        List<X509Certificate> candidates = new ArrayList<>();
        for (X509Certificate c : certs) {
            boolean isIssuer = false;
            byte[] subjEnc = safeEncodePrincipal(c.getSubjectX500Principal().getEncoded());
            for (X509Certificate other : certs) {
                if (other == c) continue;
                byte[] issuerEnc = safeEncodePrincipal(other.getIssuerX500Principal().getEncoded());
                if (java.util.Arrays.equals(subjEnc, issuerEnc)) {
                    isIssuer = true;
                    break;
                }
            }
            if (!isIssuer) candidates.add(c);
        }
        if (candidates.size() == 1) return candidates.getFirst();

        // 第二阶段：基于 BasicConstraints 筛选终端实体证书
        List<X509Certificate> endEntities = new ArrayList<>();
        for (X509Certificate c : candidates.isEmpty() ? certs : candidates) {
            try {
                if (c.getBasicConstraints() == -1) endEntities.add(c);
            } catch (Exception ignored) {
            }
        }
        if (endEntities.size() == 1) return endEntities.getFirst();

        // 第三阶段：选择 DNS SAN 数量最多的证书
        List<X509Certificate> pool = endEntities.isEmpty() ? (candidates.isEmpty() ? certs : candidates) : endEntities;
        X509Certificate best = getX509Certificate(pool);
        if (best != null) return best;

        // 第四阶段：选择最早到期的证书
        X509Certificate earliest = null;
        Date earliestDate = null;
        for (X509Certificate c : pool) {
            try {
                Date na = c.getNotAfter();
                if (earliest == null || na.before(earliestDate)) {
                    earliest = c;
                    earliestDate = na;
                }
            } catch (Exception ignored) {
            }
        }
        return earliest;
    }

    private static  X509Certificate getX509Certificate(List<X509Certificate> pool) {
        X509Certificate best = null;
        int bestSan = -1;
        for (X509Certificate c : pool) {
            int sanCount = 0;
            try {
                Collection<List<?>> alt = c.getSubjectAlternativeNames();
                if (alt != null) for (List<?> row : alt)
                    if (row.size() >= 2 && row.getFirst() instanceof Integer && ((Integer) row.getFirst()) == 2)
                        sanCount++;
            } catch (Exception ignored) {
            }
            if (sanCount > bestSan) {
                bestSan = sanCount;
                best = c;
            }
        }
        return best;
    }

    /**
     * 安全编码主体信息，避免空指针异常。
     *
     * @param enc 原始编码字节数组
     * @return 编码字节数组，若输入为 null 则返回空数组
     */
    private static byte[] safeEncodePrincipal(byte[] enc) {
        return enc == null ? new byte[0] : enc;
    }

    /**
     * 从证书中提取信息并填充到 SslInfo 对象。
     *
     * @param c    X.509 证书
     * @param info 用于存放提取结果的对象
     */
    private static void fillFromCert(X509Certificate c, SslInfo info) {
        try {
            // 提取颁发者信息
            try {
                X500Name xin = X500Name.getInstance(c.getIssuerX500Principal().getEncoded());
                RDN[] cns = xin.getRDNs(BCStyle.CN);
                if (cns != null && cns.length > 0) info.issuer = cns[0].getFirst().getValue().toString();
                RDN[] os = xin.getRDNs(BCStyle.O);
                if (os != null && os.length > 0) info.organization = os[0].getFirst().getValue().toString();
            } catch (Exception ignored) {
            }
            info.issuedAt = c.getNotBefore();
            info.expiresAt = c.getNotAfter();
            // 提取主题通用名称
            try {
                X500Name xn = X500Name.getInstance(c.getSubjectX500Principal().getEncoded());
                RDN[] rdns = xn.getRDNs(BCStyle.CN);
                if (rdns != null) for (RDN r : rdns) info.commonNames.add(r.getFirst().getValue().toString());
            } catch (Exception e) {
                // 回退到简单字符串解析
                String dn = c.getSubjectX500Principal().getName();
                String[] parts = dn.split(",");
                for (String p : parts) {
                    p = p.trim();
                    if (p.startsWith("CN=") || p.startsWith("cn=")) info.commonNames.add(p.substring(3));
                }
            }

            // 提取 DNS 类型的主题备用名称
            try {
                Collection<List<?>> alt = c.getSubjectAlternativeNames();
                if (alt != null) for (List<?> row : alt)
                    if (row.size() >= 2 && row.get(0) instanceof Integer && ((Integer) row.get(0)) == 2)
                        info.dns.add(String.valueOf(row.get(1)));
            } catch (Exception ignored) {
            }

            // 计算 SHA-256 指纹
            info.sha256Fingerprint = bytesToHex(MessageDigest.getInstance("SHA-256").digest(c.getEncoded()));
        } catch (Exception e) {
            info.extra.put("error", e.getMessage());
        }
    }

    /**
     * 将字节数组转换为十六进制字符串。
     *
     * @param b 字节数组
     * @return 十六进制字符串
     */
    private static String bytesToHex(byte[] b) {
        StringBuilder sb = new StringBuilder();
        for (byte x : b) sb.append(String.format("%02x", x & 0xff));
        return sb.toString();
    }

}
