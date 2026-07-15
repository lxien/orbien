/*
 *    Copyright 2026 lxien
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package io.github.lxien.orbien.server.transport.http;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.lxien.orbien.core.domain.BasicAuthConfig;
import io.github.lxien.orbien.core.domain.HttpUser;
import io.github.lxien.orbien.core.domain.ProxyConfig;
import io.github.lxien.orbien.core.domain.ProxyConfigExt;
import io.github.lxien.orbien.core.transport.AttributeKeys;
import io.github.lxien.orbien.core.utils.ChannelUtils;
import io.github.lxien.orbien.core.utils.StringUtils;
import io.github.lxien.orbien.server.service.ProxyConfigService;
import io.github.lxien.orbien.server.utils.NettyHttpUtils;
import io.github.lxien.orbien.server.vhost.DomainRegistry;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.CharsetUtil;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Base64;

/**
 * HTTP/HTTPS Basic Auth。BCrypt 较慢：每连接只验一次，已验证凭证按代理短缓存。
 */
@Component
@ChannelHandler.Sharable
public class BasicAuthHandler extends ChannelInboundHandlerAdapter {
    private final InternalLogger logger = InternalLoggerFactory.getInstance(BasicAuthHandler.class);

    /**
     * key = proxyId + '\\0' + Authorization
     */
    private final Cache<String, Boolean> verifiedAuthCache = Caffeine.newBuilder()
            .maximumSize(4096)
            .expireAfterWrite(Duration.ofMinutes(5))
            .build();

    @Autowired
    private ProxyConfigService proxyConfigService;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private DomainRegistry domainRegistry;

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        Channel visitor = ctx.channel();
        if (Boolean.TRUE.equals(visitor.attr(AttributeKeys.BASIC_AUTH_PASSED).get())) {
            ctx.fireChannelRead(msg);
            return;
        }

        String domain = visitor.attr(AttributeKeys.VISIT_DOMAIN).get();
        String proxyId = domainRegistry.getProxyIdByDomain(domain);
        if (!StringUtils.hasText(proxyId)) {
            ctx.fireChannelRead(msg);
            return;
        }

        ProxyConfigExt ext = proxyConfigService.findById(proxyId);
        if (ext == null || ext.getProxyConfig().isFile()) {
            markPassed(ctx);
            ctx.fireChannelRead(msg);
            return;
        }

        ProxyConfig config = ext.getProxyConfig();
        BasicAuthConfig basicAuth = config.getBasicAuth();
        if (basicAuth == null || !basicAuth.isEnabled()) {
            markPassed(ctx);
            ctx.fireChannelRead(msg);
            return;
        }

        String basicAuthHeader = visitor.attr(AttributeKeys.BASIC_AUTH_HEADER).get();
        if (basicAuthHeader == null || !isBasicScheme(basicAuthHeader)) {
            ReferenceCountUtil.release(msg);
            sendBasicAuth(visitor);
            return;
        }

        String cacheKey = cacheKey(proxyId, basicAuthHeader);
        if (Boolean.TRUE.equals(verifiedAuthCache.getIfPresent(cacheKey))) {
            markPassed(ctx);
            ctx.fireChannelRead(msg);
            return;
        }

        try {
            String base64Credentials = basicAuthHeader.substring(6).trim();
            String credentials = new String(Base64.getDecoder().decode(base64Credentials), CharsetUtil.UTF_8);
            String[] parts = credentials.split(":", 2);
            if (parts.length != 2 || !check(parts[0], parts[1], basicAuth)) {
                ReferenceCountUtil.release(msg);
                sendBasicAuth(visitor);
                return;
            }
            verifiedAuthCache.put(cacheKey, Boolean.TRUE);
            markPassed(ctx);
            ctx.fireChannelRead(msg);
        } catch (Exception e) {
            logger.debug("Basic Auth 解码失败: {}", e.getMessage());
            ReferenceCountUtil.release(msg);
            sendBasicAuth(visitor);
        }
    }

    private void markPassed(ChannelHandlerContext ctx) {
        ctx.channel().attr(AttributeKeys.BASIC_AUTH_PASSED).set(Boolean.TRUE);
        if (ctx.pipeline().context(this) != null) {
            ctx.pipeline().remove(this);
        }
    }

    private static boolean isBasicScheme(String header) {
        return header.length() >= 6 && header.regionMatches(true, 0, "Basic ", 0, 6);
    }

    private void sendBasicAuth(Channel visitor) {
        NettyHttpUtils.sendBasicAuth(visitor).addListener(future -> ChannelUtils.closeOnFlush(visitor));
    }

    private boolean check(String username, String password, BasicAuthConfig basicAuth) {
        HttpUser user = basicAuth.getUser(username);
        return user != null && passwordEncoder.matches(password, user.getPassword());
    }

    private static String cacheKey(String proxyId, String authorizationHeader) {
        return proxyId + '\0' + authorizationHeader;
    }

    public void invalidate(String proxyId) {
        if (!StringUtils.hasText(proxyId)) {
            return;
        }
        String prefix = proxyId + '\0';
        verifiedAuthCache.asMap().keySet().removeIf(key -> key.startsWith(prefix));
    }
}
