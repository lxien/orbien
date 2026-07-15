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
package io.github.lxien.orbien.server.transport.bridge;

import io.github.lxien.orbien.core.domain.HeaderRewriteConfig;
import io.github.lxien.orbien.core.domain.HeaderRewriteRule;
import io.github.lxien.orbien.core.domain.ProxyConfig;
import io.github.lxien.orbien.core.domain.ProxyConfigExt;
import io.github.lxien.orbien.core.http.HeaderRewriteSupport;
import io.github.lxien.orbien.core.transport.AbstractTunnelBridgeDecorator;
import io.github.lxien.orbien.core.transport.TunnelBridge;
import io.github.lxien.orbien.core.transport.VisitorAddressResolver;
import io.github.lxien.orbien.core.utils.StringUtils;
import io.github.lxien.orbien.server.service.ProxyConfigService;
import io.github.lxien.orbien.server.statemachine.stream.StreamContext;
import io.github.lxien.orbien.server.transport.http.HttpHeaderBlockProcessor;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.handler.ssl.SslHandler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 改写后端 -> 访客的 HTTP 响应头
 */
public class HeaderRewriteResponseBridge extends AbstractTunnelBridgeDecorator {
    private final ProxyConfigService proxyConfigService;
    private final String proxyId;
    private final String scheme;
    private final String host;
    private final String clientIp;
    private final HttpHeaderBlockProcessor processor = new HttpHeaderBlockProcessor();
    private ByteBuf pending;

    public HeaderRewriteResponseBridge(TunnelBridge delegate,
                                       StreamContext context,
                                       ProxyConfigService proxyConfigService) {
        super(delegate);
        this.proxyConfigService = proxyConfigService;
        this.proxyId = context.getProxyId();
        this.scheme = resolveScheme(context);
        this.host = context.getVisitorDomain();
        this.clientIp = VisitorAddressResolver.resolveIp(context.getVisitor());
    }

    @Override
    public void forwardToRemote(ByteBuf payload, boolean sharedWithInbound) {
        if (!StringUtils.hasText(proxyId) || processor.mode() == HttpHeaderBlockProcessor.Mode.PASSTHROUGH) {
            delegate.forwardToRemote(payload, sharedWithInbound);
            return;
        }

        ByteBuf incoming = sharedWithInbound ? payload.retainedDuplicate() : payload;
        ByteBuf buf;
        if (pending != null) {
            pending.writeBytes(incoming);
            incoming.release();
            buf = pending;
            pending = null;
        } else {
            buf = incoming;
        }

        try {
            process(buf);
        } catch (RuntimeException e) {
            releaseQuietly(buf);
            releaseQuietly(pending);
            pending = null;
            throw e;
        }
    }

    private void process(ByteBuf buf) {
        while (buf.isReadable()) {
            if (processor.mode() == HttpHeaderBlockProcessor.Mode.PASSTHROUGH) {
                delegate.forwardToRemote(buf, false);
                return;
            }
            if (processor.mode() == HttpHeaderBlockProcessor.Mode.SKIP_BODY) {
                int take = processor.feedBody(buf);
                if (take <= 0) {
                    stash(buf);
                    return;
                }
                delegate.forwardToRemote(buf.readRetainedSlice(take), false);
                continue;
            }

            List<HeaderRewriteRule> rules = loadResponseRules();
            Map<String, String> vars = HeaderRewriteSupport.buildVars(clientIp, scheme, host);
            int readerBefore = buf.readerIndex();
            byte[] rewritten;
            try {
                rewritten = processor.tryRewriteHeader(buf, false, rules, vars);
            } catch (IllegalStateException e) {
                buf.readerIndex(readerBefore);
                processor.setPassthrough();
                delegate.forwardToRemote(buf, false);
                return;
            }
            if (rewritten == null) {
                if (processor.mode() == HttpHeaderBlockProcessor.Mode.PASSTHROUGH) {
                    buf.readerIndex(readerBefore);
                    delegate.forwardToRemote(buf, false);
                    return;
                }
                buf.readerIndex(readerBefore);
                stash(buf);
                return;
            }

            delegate.forwardToRemote(Unpooled.wrappedBuffer(rewritten), false);
            if (buf.isReadable() && processor.mode() == HttpHeaderBlockProcessor.Mode.SKIP_BODY) {
                int take = processor.feedBody(buf);
                if (take > 0) {
                    delegate.forwardToRemote(buf.readRetainedSlice(take), false);
                }
            }
        }
        buf.release();
    }

    private List<HeaderRewriteRule> loadResponseRules() {
        ProxyConfigExt ext = proxyConfigService.findById(proxyId);
        if (ext == null || ext.getProxyConfig() == null) {
            return Collections.emptyList();
        }
        ProxyConfig config = ext.getProxyConfig();
        HeaderRewriteConfig rewrite = config.getHeaderRewrite();
        if (rewrite == null || !rewrite.hasResponseRules()) {
            return Collections.emptyList();
        }
        return new ArrayList<>(rewrite.getResponseRulesView());
    }

    private void stash(ByteBuf buf) {
        if (!buf.isReadable()) {
            buf.release();
            return;
        }
        pending = buf.alloc().buffer(buf.readableBytes());
        pending.writeBytes(buf);
        buf.release();
    }

    private static void releaseQuietly(ByteBuf buf) {
        if (buf != null && buf.refCnt() > 0) {
            buf.release();
        }
    }

    private static String resolveScheme(StreamContext context) {
        Channel visitor = context.getVisitor();
        if (visitor != null && visitor.pipeline().get(SslHandler.class) != null) {
            return "https";
        }
        Integer port = context.getListenerPort();
        if (port != null && port == 443) {
            return "https";
        }
        return "http";
    }

    /**
     * HTTP 流一律包装
     * 规则每条响应从缓存加载
     */
    public static TunnelBridge wrapIfNeeded(TunnelBridge bridge,
                                            StreamContext context,
                                            ProxyConfigService proxyConfigService) {
        if (bridge == null || context == null || proxyConfigService == null) {
            return bridge;
        }
        if (context.getProtocol() == null || !context.getProtocol().isHttp()) {
            return bridge;
        }
        if (!StringUtils.hasText(context.getProxyId())) {
            return bridge;
        }
        return new HeaderRewriteResponseBridge(bridge, context, proxyConfigService);
    }
}
