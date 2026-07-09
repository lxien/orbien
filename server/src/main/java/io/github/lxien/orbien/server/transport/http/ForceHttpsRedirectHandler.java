package io.github.lxien.orbien.server.transport.http;

import io.github.lxien.orbien.core.utils.StringUtils;
import io.github.lxien.orbien.core.domain.ProxyConfig;
import io.github.lxien.orbien.core.domain.ProxyConfigExt;
import io.github.lxien.orbien.core.http.ForceHttpsPolicy;
import io.github.lxien.orbien.core.transport.AttributeKeys;
import io.github.lxien.orbien.core.utils.ChannelUtils;
import io.github.lxien.orbien.server.config.AppConfig;
import io.github.lxien.orbien.server.service.ProxyConfigService;
import io.github.lxien.orbien.server.utils.NettyHttpUtils;
import io.github.lxien.orbien.server.vhost.DomainRegistry;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * HTTP 端口（明文）访问 HTTPS 代理域名时的强制跳转处理。
 * <p>
 * 仅对 {@code protocol=https && force_https=true（默认）} 生效；不建立到内网的隧道。
 */
@Component
@ChannelHandler.Sharable
public class ForceHttpsRedirectHandler extends ChannelInboundHandlerAdapter {

    private final InternalLogger logger = InternalLoggerFactory.getInstance(ForceHttpsRedirectHandler.class);

    @Resource
    private AppConfig appConfig;
    @Autowired
    private DomainRegistry domainRegistry;
    @Autowired
    private ProxyConfigService proxyConfigService;

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (!(msg instanceof ByteBuf buf)) {
            ctx.fireChannelRead(msg);
            return;
        }
        Channel visitor = ctx.channel();
        String domain = visitor.attr(AttributeKeys.VISIT_DOMAIN).get();
        if (!StringUtils.hasText(domain)) {
            ctx.fireChannelRead(msg);
            return;
        }

        String proxyId = domainRegistry.getProxyIdByDomain(domain);
        if (!StringUtils.hasText(proxyId)) {
            ctx.fireChannelRead(msg);
            return;
        }

        ProxyConfigExt ext = proxyConfigService.findById(proxyId);
        if (ext == null || ext.getProxyConfig() == null) {
            ctx.fireChannelRead(msg);
            return;
        }

        ProxyConfig config = ext.getProxyConfig();
        if (!config.isHttps()) {
            ctx.fireChannelRead(msg);
            return;
        }

        if (ForceHttpsPolicy.isRedirectEnabled(config)) {
            HttpFirstLineParser.HttpFirstLine firstLine = HttpFirstLineParser.parse(buf);
            if (firstLine != null && !ForceHttpsPolicy.isAcmeChallengePath(firstLine.requestUri())) {
                String location = ForceHttpsPolicy.buildHttpsLocation(
                        domain, appConfig.getHttpsProxyPort(), firstLine.requestUri());
                logger.debug("强制 HTTPS 跳转 domain={} {} -> {}", domain, firstLine.requestUri(), location);
                ReferenceCountUtil.release(buf);
                NettyHttpUtils.sendRedirect(visitor, ForceHttpsPolicy.DEFAULT_REDIRECT_STATUS, location)
                        .addListener(f -> ChannelUtils.closeOnFlush(visitor));
                return;
            }
            if (firstLine == null) {
                ctx.fireChannelRead(msg);
                return;
            }
            ctx.fireChannelRead(msg);
            return;
        }

        // HTTPS 代理但关闭了强制跳转：拒绝在明文 HTTP 端口访问
        logger.debug("HTTPS 代理 {} 未开启 force_https，拒绝明文 HTTP 访问 domain={}", config.getName(), domain);
        ReferenceCountUtil.release(buf);
        NettyHttpUtils.sendHttp403(visitor).addListener(f -> ChannelUtils.closeOnFlush(visitor));
    }
}
