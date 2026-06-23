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

package com.xiaoniucode.etp.server.transport.https;


import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.xiaoniucode.etp.server.security.SelfSignedCertificateGenerator;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.File;
import java.nio.file.Paths;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * SSL 证书管理器
 */
@Component
public class SslCertificateManager {
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(SslCertificateManager.class);
    private final ConcurrentHashMap<String/*domain*/, SslContext> certMap = new ConcurrentHashMap<>();
    private final Cache<String, SslContext> l1Cache = Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterAccess(1, TimeUnit.HOURS)
            .build();
    /**
     * 通配符域名列表
     */
    private final Set<String> wildcardDomains = ConcurrentHashMap.newKeySet();
    /**
     * 用于记录已经被部署的域名
     */
    private final Set<String> deployedDomains = ConcurrentHashMap.newKeySet();
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
            File keyFile = new File(DEFAULT_SSL_PATH, "privkey.pem");
            File certFile = new File(DEFAULT_SSL_PATH, "fullchain.pem");
            if (certFile.exists() && keyFile.exists()) {
                this.defaultSslContext = SslContextBuilder.forServer(certFile, keyFile).build();
                logger.info("默认SSL证书已加载");
            } else {
                SelfSignedCertificateGenerator.Result result = SelfSignedCertificateGenerator.generate();
                this.defaultSslContext = SslContextBuilder.forServer(result.privateKey(), result.certificate()).build();
                SelfSignedCertificateGenerator.writeToPemFiles(result,keyFile,certFile);
                logger.info("生成HTTPS自签名SSL证书");
            }
        } catch (Exception e) {
            logger.error("证书加载错误", e);
        }
    }

    /**
     * 部署证书到指定域名
     */
    public void deploy(String domain, File certFile, File keyFile) throws Exception {
        //todo  构建 SslContext  路径
        SslContext sslCtx = SslContextBuilder.forServer(
                new File(DEFAULT_SSL_PATH, "fullchain.pem"),
                new File(DEFAULT_SSL_PATH, "privkey.pem")
        ).build();

        // 更新索引和缓存
        deployedDomains.add(domain);
        if (domain.startsWith("*.")) {
            wildcardDomains.add(domain);
        }
        l1Cache.put(domain, sslCtx);

        logger.info("证书已部署: {}", domain);
        logger.info("证书已部署到域名: {}", domain);
    }
    public void cancelDeploy(String domain) {
        l1Cache.invalidate(domain);
        deployedDomains.remove(domain);
        wildcardDomains.remove(domain);
    }

    /**
     * 根据 SNI 域名获取对应的 SslContext
     */
    public SslContext getSslContext(String sniHost) {
        if (!StringUtils.hasText(sniHost)) return defaultSslContext;
        //从缓存读取
        SslContext ctx = l1Cache.getIfPresent(sniHost);
        if (ctx != null) return ctx;
        //判断域名是否部署，没有部署直接返回默认证书
        if (!deployedDomains.contains(sniHost) && !matchWildcard(sniHost)) {
            return defaultSslContext;  // 从未部署，零文件IO
        }
        //todo 已经部署但缓存过期，从文件系统加载证书

        return defaultSslContext;
    }
    /**
     * todo 通配符匹配 可能需要优化时间复杂度
     */
    private boolean matchWildcard(String sniHost) {
        for (String wildcard : wildcardDomains) {
            String suffix = wildcard.substring(1);
            if (sniHost.endsWith(suffix)) {
                return true;
            }
        }
        return false;
    }
    /**
     * 移除证书
     */
    public void remove(String domain) {
        certMap.remove(domain);
    }
}