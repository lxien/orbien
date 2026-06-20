/*
 *
 *  *    Copyright 2026 xiaoniucode
 *  *
 *  *    Licensed under the Apache License, Version 2.0 (the "License");
 *  *    you may not use this file except in compliance with the License.
 *  *    You may obtain a copy of the License at
 *  *
 *  *        http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  *    Unless required by applicable law or agreed to in writing, software
 *  *    distributed under the License is distributed on an "AS IS" BASIS,
 *  *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  *    See the License for the specific language governing permissions and
 *  *    limitations under the License.
 *
 */

package com.xiaoniucode.etp.server.transport.https.ssl;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.File;
import java.nio.file.Paths;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SSL 证书管理器
 */
@Component
public class SslCertificateManager {
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(SslCertificateManager.class);
    private final ConcurrentHashMap<String/*domain*/, SslContext> certMap = new ConcurrentHashMap<>();
    private SslContext defaultSslContext;
    private static final String DEFAULT_SSL_PATH = Paths.get("cert", "system", "gateway").toString();

    /**
     * 系统首次启动时执行
     * 1.加载默认证书，如果不存在则自动生成
     * 2.加载部署可用域名证书
     */
    @PostConstruct
    public void init() {
        try {
            File certFile = new File(DEFAULT_SSL_PATH, "fullchain.pem");
            File keyFile = new File(DEFAULT_SSL_PATH, "privkey.pem");
            if (certFile.exists() && keyFile.exists()) {
                this.defaultSslContext = SslContextBuilder.forServer(certFile, keyFile).build();
                logger.info("默认SSL证书已加载: {}", certFile.getAbsolutePath());
            } else {
                //todo 如果没有证书则自动生成
            }
        } catch (Exception e) {
            logger.error("证书加载错误", e);
        }
    }

    /**
     * 部署证书到指定域名
     */
    public void deploy(String domain, File certFile, File keyFile) throws Exception {
        SslContext sslCtx = SslContextBuilder.forServer(certFile, keyFile).build();
        certMap.put(domain, sslCtx);
        logger.info("证书已部署到域名: {}", domain);
    }

    /**
     * 根据 SNI 域名获取对应的 SslContext
     */
    public SslContext getSslContext(String sniHost) {
        if (!StringUtils.hasText(sniHost)) return defaultSslContext;
        SslContext ctx = certMap.get(sniHost);
        if (ctx != null) return ctx;
        return defaultSslContext;
    }

    /**
     * 移除证书
     */
    public void remove(String domain) {
        certMap.remove(domain);
    }
}