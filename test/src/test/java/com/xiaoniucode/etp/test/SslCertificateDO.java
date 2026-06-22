

package com.xiaoniucode.etp.test;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class SslCertificateDO {

    /**
     * 证书分类
     * 例如：Let's Encrypt
     */
    private String issuer;

    /**
     * 证书品牌
     * 例如：YR2
     */
    private String issuer0;

    /**
     * 主域名
     */
    private String subject;

    /**
     * SAN域名，可能多个，用逗号分隔
     */
    private String sanDomains;

    /**
     * 生效时间
     */
    private LocalDateTime notBefore;

    /**
     * 过期时间
     */
    private LocalDateTime notAfter;

    /**
     * 指纹（SHA-256）
     */
    private String fingerprint;

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public String getIssuer0() {
        return issuer0;
    }

    public void setIssuer0(String issuer0) {
        this.issuer0 = issuer0;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getSanDomains() {
        return sanDomains;
    }

    public void setSanDomains(String sanDomains) {
        this.sanDomains = sanDomains;
    }

    public LocalDateTime getNotBefore() {
        return notBefore;
    }

    public void setNotBefore(LocalDateTime notBefore) {
        this.notBefore = notBefore;
    }

    public LocalDateTime getNotAfter() {
        return notAfter;
    }

    public void setNotAfter(LocalDateTime notAfter) {
        this.notAfter = notAfter;
    }

    public String getFingerprint() {
        return fingerprint;
    }

    public void setFingerprint(String fingerprint) {
        this.fingerprint = fingerprint;
    }

    @Override
    public String toString() {
        return "SslCertificateDO {" +
                "\n  证书分类: '" + issuer + '\'' +
                "\n  证书品牌: '" + issuer0 + '\'' +
                "\n  主域名: '" + subject + '\'' +
                "\n  SAN域名: '" + sanDomains + '\'' +
                "\n  生效时间: " + notBefore +
                "\n  过期时间: " + notAfter +
                "\n  指纹: '" + fingerprint + '\'' +
                "\n}";
    }
}
