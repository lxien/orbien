package io.github.lxien.orbien.core.domain;

import io.netty.handler.ssl.ClientAuth;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@EqualsAndHashCode
public class TlsConfig {
    private boolean enabled;
    private String certFile;
    private String keyFile;
    private String caFile;
    private String keyPassword;

    public TlsConfig(boolean enabled) {
        this.enabled = enabled;
    }

    public TlsConfig(boolean enabled, String certFile, String keyFile, String caFile, String keyPassword) {
        this.enabled = enabled;
        this.certFile = certFile;
        this.keyFile = keyFile;
        this.caFile = caFile;
        this.keyPassword = keyPassword;
    }

    /**
     * 获取客户端认证模式
     *
     * @return 客户端认证模式
     */
    public ClientAuth getClientAuthMode() {
        if (caFile != null && !caFile.isEmpty()) {
            return ClientAuth.REQUIRE;
        } else {
            return ClientAuth.NONE;
        }
    }

    /**
     * 是否启用双向认证
     *
     * @return 是否启用双向认证
     */
    public boolean mTLSEnabled() {
        return caFile != null && !caFile.isEmpty();
    }
}
