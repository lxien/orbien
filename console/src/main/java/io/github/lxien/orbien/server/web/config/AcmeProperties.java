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
     * ACME 环境：staging（Let's Encrypt 测试）或 production（生产）。
     * 未显式配置 directory-url 时，将根据该值选择默认 ACME 目录地址。
     */
    private Environment environment = Environment.STAGING;

    /**
     * 可选：直接指定 ACME 目录地址，支持 acme:// 协议。
     * 示例：acme://letsencrypt.org/staging、acme://letsencrypt.org
     */
    private String directoryUrl;

    /**
     * 可选：ACME 账户私钥路径；未配置时按 environment 分目录存储。
     */
    private String accountKeyPath;

    private String credentialMasterKey = "orbien-default-credential-key-32bytes!!";
    private int dnsPollIntervalSeconds = 15;
    private int dnsPollMaxAttempts = 40;

    public enum Environment {
        STAGING,
        PRODUCTION
    }

    public String resolveDirectoryUrl() {
        if (StringUtils.hasText(directoryUrl)) {
            return directoryUrl.trim();
        }
        return environment == Environment.PRODUCTION
                ? "acme://letsencrypt.org"
                : "acme://letsencrypt.org/staging";
    }

    public String resolveAccountKeyPath() {
        if (StringUtils.hasText(accountKeyPath)) {
            return accountKeyPath.trim();
        }
        return "cert/acme/" + environment.name().toLowerCase() + "/account.key";
    }
}
