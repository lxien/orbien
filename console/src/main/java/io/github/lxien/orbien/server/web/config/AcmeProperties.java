package io.github.lxien.orbien.server.web.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Data
@Component
@ConfigurationProperties(prefix = "orbien.acme")
public class AcmeProperties {

    /**
     * Let's Encrypt 生产目录
     */
    public static final String LETSENCRYPT_PRODUCTION_DIRECTORY = "https://acme-v02.api.letsencrypt.org/directory";
    /**
     * Let's Encrypt staging 目录
     */
    public static final String LETSENCRYPT_STAGING_DIRECTORY = "https://acme-staging-v02.api.letsencrypt.org/directory";

    /**
     * 未显式配置 directory-url 时，将根据该值选择默认 ACME 目录地址。
     */
    private Environment environment = Environment.STAGING;

    /**
     * 直接指定 ACME 目录地址。
     * 示例：https://acme-v02.api.letsencrypt.org/directory
     */
    private String directoryUrl;

    /**
     *  ACME 账户私钥路径；未配置时按 environment 分目录存储。
     */
    private String accountKeyPath;

    private String credentialMasterKey = "orbien-default-credential-key-32bytes!!";
    private int dnsPollIntervalSeconds = 15;
    /**
     * DNS TXT 传播等待（本地探测）
     */
    private int dnsPropagationMaxAttempts = 20;
    /**
     * 连续 DNS 解析失败多少次后提前结束（域名不存在等）
     */
    private int dnsPropagationFailFastAttempts = 3;
    /**
     * CA 授权轮询
     */
    private int dnsPollMaxAttempts = 20;

    public enum Environment {
        /**
         * 测试环境
         */
        STAGING,
        /**
         * 生产环境
         */
        PRODUCTION
    }

    public String resolveDirectoryUrl() {
        if (StringUtils.hasText(directoryUrl)) {
            return normalizeDirectoryUrl(directoryUrl.trim());
        }
        return environment == Environment.PRODUCTION
                ? LETSENCRYPT_PRODUCTION_DIRECTORY
                : LETSENCRYPT_STAGING_DIRECTORY;
    }

    /**
     * 将 acme:// 快捷 URI 转为 https 目录 URL，避免依赖 SPI 加载 CA Provider。
     */
    static String normalizeDirectoryUrl(String url) {
        if ("acme://letsencrypt.org".equals(url)
                || "acme://letsencrypt.org/".equals(url)
                || "acme://letsencrypt.org/v02".equals(url)) {
            return LETSENCRYPT_PRODUCTION_DIRECTORY;
        }
        if ("acme://letsencrypt.org/staging".equals(url)) {
            return LETSENCRYPT_STAGING_DIRECTORY;
        }
        return url;
    }

    public String resolveAccountKeyPath() {
        if (StringUtils.hasText(accountKeyPath)) {
            return accountKeyPath.trim();
        }
        return "cert/acme/" + environment.name().toLowerCase() + "/account.key";
    }
}
