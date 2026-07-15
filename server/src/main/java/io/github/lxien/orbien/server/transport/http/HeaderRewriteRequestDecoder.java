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

import io.github.lxien.orbien.core.domain.HeaderRewriteConfig;
import io.github.lxien.orbien.core.domain.HeaderRewriteRule;
import io.github.lxien.orbien.core.domain.ProxyConfig;
import io.github.lxien.orbien.core.domain.ProxyConfigExt;
import io.github.lxien.orbien.core.http.HeaderRewriteSupport;
import io.github.lxien.orbien.core.transport.AttributeKeys;
import io.github.lxien.orbien.core.transport.VisitorAddressResolver;
import io.github.lxien.orbien.core.utils.ChannelUtils;
import io.github.lxien.orbien.core.utils.StringUtils;
import io.github.lxien.orbien.server.service.ProxyConfigService;
import io.github.lxien.orbien.server.vhost.DomainRegistry;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.CompositeByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 按代理规则改写 HTTP 请求头。每个请求头块都会重新加载规则，避免 keep-alive 粘住旧配置
 */
public class HeaderRewriteRequestDecoder extends ByteToMessageDecoder {
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(HeaderRewriteRequestDecoder.class);

    private final ProxyConfigService proxyConfigService;
    private final DomainRegistry domainRegistry;
    private final String scheme;
    private final HttpHeaderBlockProcessor processor = new HttpHeaderBlockProcessor();

    public HeaderRewriteRequestDecoder(ProxyConfigService proxyConfigService,
                                       DomainRegistry domainRegistry,
                                       String scheme) {
        this.proxyConfigService = proxyConfigService;
        this.domainRegistry = domainRegistry;
        this.scheme = scheme == null ? "http" : scheme;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        if (!in.isReadable()) {
            return;
        }
        if (processor.mode() == HttpHeaderBlockProcessor.Mode.PASSTHROUGH) {
            out.add(in.readRetainedSlice(in.readableBytes()));
            return;
        }

        try {
            while (in.isReadable()) {
                if (processor.mode() == HttpHeaderBlockProcessor.Mode.PASSTHROUGH) {
                    out.add(in.readRetainedSlice(in.readableBytes()));
                    return;
                }
                if (processor.mode() == HttpHeaderBlockProcessor.Mode.SKIP_BODY) {
                    int take = processor.feedBody(in);
                    if (take <= 0) {
                        return;
                    }
                    out.add(in.readRetainedSlice(take));
                    continue;
                }

                List<HeaderRewriteRule> rules = loadRequestRules(ctx.channel());
                Channel visitor = ctx.channel();
                Map<String, String> vars = HeaderRewriteSupport.buildVars(
                        VisitorAddressResolver.resolveIp(visitor),
                        scheme,
                        visitor.attr(AttributeKeys.VISIT_DOMAIN).get());

                byte[] rewritten = processor.tryRewriteHeader(in, true, rules, vars);
                if (rewritten == null) {
                    if (processor.mode() == HttpHeaderBlockProcessor.Mode.PASSTHROUGH) {
                        out.add(in.readRetainedSlice(in.readableBytes()));
                    }
                    return;
                }

                ByteBuf headerBuf = ctx.alloc().buffer(rewritten.length);
                headerBuf.writeBytes(rewritten);

                if (in.isReadable() && processor.mode() == HttpHeaderBlockProcessor.Mode.SKIP_BODY) {
                    int take = processor.feedBody(in);
                    if (take > 0) {
                        ByteBuf body = in.readRetainedSlice(take);
                        CompositeByteBuf composite = ctx.alloc().compositeBuffer(2);
                        composite.addComponents(true, headerBuf, body);
                        out.add(composite);
                    } else {
                        out.add(headerBuf);
                    }
                } else {
                    out.add(headerBuf);
                }
            }
        } catch (IllegalStateException e) {
            logger.debug("[HTTP] 请求头改写失败: {}", e.getMessage());
            ChannelUtils.closeOnFlush(ctx.channel());
        }
    }

    private List<HeaderRewriteRule> loadRequestRules(Channel visitor) {
        String domain = visitor.attr(AttributeKeys.VISIT_DOMAIN).get();
        if (!StringUtils.hasText(domain)) {
            return Collections.emptyList();
        }
        String proxyId = domainRegistry.getProxyIdByDomain(domain);
        if (!StringUtils.hasText(proxyId)) {
            return Collections.emptyList();
        }
        ProxyConfigExt ext = proxyConfigService.findById(proxyId);
        if (ext == null || ext.getProxyConfig() == null) {
            return Collections.emptyList();
        }
        ProxyConfig config = ext.getProxyConfig();
        HeaderRewriteConfig rewrite = config.getHeaderRewrite();
        if (rewrite == null || !rewrite.hasRequestRules()) {
            return Collections.emptyList();
        }
        return new ArrayList<>(rewrite.getRequestRulesView());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        Throwable root = cause;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        if (root instanceof javax.net.ssl.SSLException) {
            logger.debug("[HTTPS] TLS 握手失败: {}", root.getMessage());
        } else {
            logger.error("[HTTP] HeaderRewriteRequestDecoder 异常", cause);
        }
        ChannelUtils.closeOnFlush(ctx.channel());
    }
}
