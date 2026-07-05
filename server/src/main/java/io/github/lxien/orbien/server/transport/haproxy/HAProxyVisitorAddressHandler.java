package io.github.lxien.orbien.server.transport.haproxy;

import io.github.lxien.orbien.core.transport.AttributeKeys;
import io.github.lxien.orbien.core.transport.VisitorAddressResolver;
import io.github.lxien.orbien.server.config.domain.ProxyProtocolConfig;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.haproxy.HAProxyMessage;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

/**
 * 消费 HAProxyMessageDecoder 输出的 PROXY 头，写入 {@link AttributeKeys#VISITOR_REAL_IP}
 * @author lxien
 */
public class HAProxyVisitorAddressHandler extends ChannelInboundHandlerAdapter {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(HAProxyVisitorAddressHandler.class);

    private final TrustedProxyMatcher trustedProxyMatcher;

    public HAProxyVisitorAddressHandler(ProxyProtocolConfig config) {
        this.trustedProxyMatcher = new TrustedProxyMatcher(config.getTrustedProxies());
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof HAProxyMessage proxy) {
            try {
                applyProxyAddress(ctx, proxy);
            } finally {
                ReferenceCountUtil.release(proxy);
            }
            return;
        }
        ctx.fireChannelRead(msg);
    }

    private void applyProxyAddress(ChannelHandlerContext ctx, HAProxyMessage proxy) {
        String peerIp = VisitorAddressResolver.resolveRemoteOnly(ctx.channel());
        if (!trustedProxyMatcher.isTrustedPeer(peerIp)) {
            logger.warn("[HAProxy] 忽略不可信来源的 PROXY 头 peer={}", peerIp);
            return;
        }
        String sourceIp = proxy.sourceAddress();
        if (sourceIp == null || sourceIp.isBlank()) {
            return;
        }
        ctx.channel().attr(AttributeKeys.VISITOR_REAL_IP).set(sourceIp.trim());
        logger.debug("[HAProxy] 真实访客 IP={} peer={} port={}", sourceIp, peerIp, proxy.sourcePort());
    }
}
