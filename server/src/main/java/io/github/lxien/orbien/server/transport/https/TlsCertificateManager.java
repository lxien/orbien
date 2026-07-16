/*
 *
 *  *    Copyright 2026 lxien
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

package io.github.lxien.orbien.server.transport.https;


import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.lxien.orbien.server.config.SystemConstants;
import io.github.lxien.orbien.server.security.SelfSignedCertificateGenerator;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.File;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * SSL 证书管理器
 */
@Component
public class TlsCertificateManager {
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(TlsCertificateManager.class);
    private final Cache<String/*domain*/, SslContext> l1Cache = Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterAccess(1, TimeUnit.HOURS)
            .build();
    private final Map<String/*domain*/, String/*certId*/> activeCert = new ConcurrentHashMap<>();
    private volatile SslContext defaultSslContext;
    private final ReadWriteLock rwLock = new ReentrantReadWriteLock();

    /**
     * 加载默认证书，如果不存在则自动生成并持久化到磁盘
     */
    @PostConstruct
    public void init() {
        File keyFile = new File(SystemConstants.DEFAULT_GATEWAY_TLS_CERT_PATH, "privkey.pem");
        File certFile = new File(SystemConstants.DEFAULT_GATEWAY_TLS_CERT_PATH, "fullchain.pem");
        try {
            if (certFile.isFile() && keyFile.isFile()) {
                this.defaultSslContext = SslContextBuilder.forServer(certFile, keyFile).build();
                logger.info("默认TLS 证书已加载: {}", certFile.getAbsolutePath());
                return;
            }

            // 仅有一方存在时视为损坏，清理后重新生成
            if (certFile.exists() || keyFile.exists()) {
                logger.warn("默认TLS 证书不完整(cert={}, key={})，将重新生成",
                        certFile.exists(), keyFile.exists());
                deleteQuietly(certFile);
                deleteQuietly(keyFile);
            }

            SelfSignedCertificateGenerator.Result result = SelfSignedCertificateGenerator.generate();
            SelfSignedCertificateGenerator.writeToPemFiles(result, keyFile, certFile);
            this.defaultSslContext = SslContextBuilder.forServer(result.privateKey(), result.certificate()).build();
            logger.info("已生成并持久化HTTPS自签名TLS 证书: {}", certFile.getAbsolutePath());
        } catch (Exception e) {
            logger.error("HTTPS默认证书加载错误", e);
        }
    }

    private void deleteQuietly(File file) {
        if (file != null && file.exists() && !file.delete()) {
            logger.warn("无法删除损坏的证书文件: {}", file.getAbsolutePath());
        }
    }

    public void addDeployedDomains(String certId,Set<String> domains) {
        for (String domain : domains) {
            activeCert.put(domain,certId);
        }
    }

    /**
     * 部署证书到域名
     *
     * @param domain   具体域名
     * @param keyFile  私钥
     * @param certFile 证书
     * @throws Exception
     */
    public void deploy(String certId, String domain, File keyFile, File certFile) throws Exception {
        rwLock.writeLock().lock();
        try {
            SslContext sslCtx = SslContextBuilder.forServer(certFile, keyFile).build();
            l1Cache.put(domain, sslCtx);
            activeCert.put(domain, certId);
            logger.info("证书已部署到域名: {}", domain);
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    /**
     * 取消证书部署
     *
     * @param domain 具体域名
     */
    public void cancelDeploy(String domain) {
        rwLock.writeLock().lock();
        try {
            activeCert.remove(domain);
            l1Cache.invalidate(domain);
            logger.info("域名证书已取消部署: {}", domain);
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    /**
     * 根据 SNI 具体域名获取对应的 SslContext
     */
    public SslContext getSslContext(String domain) {
        rwLock.readLock().lock();
        try {
            if (!StringUtils.hasText(domain)) {
                logger.debug("SNI 域名为空，使用默认证书");
                return defaultSslContext;
            }
            SslContext ctx = l1Cache.getIfPresent(domain);
            if (ctx != null) {
                logger.debug("从缓存获取证书成功: {}", domain);
                return ctx;
            }
            ctx = loadFromFileSystem(domain);
            if (ctx != null) {
                logger.debug("从文件系统加载证书成功: {}", domain);
                l1Cache.put(domain, ctx);
                return ctx;
            }

            return defaultSslContext;
        } finally {
            rwLock.readLock().unlock();
        }
    }

    private SslContext loadFromFileSystem(String domain) {
        try {
            logger.debug("尝试从文件系统加载证书: {}", domain);
            String certId = activeCert.get(domain);
            if (certId == null) {
                return null;
            }
            File domainDir = new File(SystemConstants.DEFAULT_DOMAIN_TLS_CERT_PATH, certId);
            File certFile = new File(domainDir, "fullchain.pem");
            File keyFile = new File(domainDir, "privkey.pem");

            if (!certFile.exists() || !keyFile.exists()) {
                logger.debug("域名 {} 的证书文件不完整，cert存在={}, key存在={}",
                        domain, certFile.exists(), keyFile.exists());
                return null;
            }
            SslContext sslCtx = SslContextBuilder.forServer(certFile, keyFile).build();
            logger.debug("从文件系统加载证书成功: {}", domain);
            return sslCtx;
        } catch (Exception e) {
            logger.debug("从文件系统加载证书失败: {}", domain, e);
            return null;
        }
    }
}