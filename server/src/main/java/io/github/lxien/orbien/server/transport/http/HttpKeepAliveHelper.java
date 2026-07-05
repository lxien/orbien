package io.github.lxien.orbien.server.transport.http;

import io.github.lxien.orbien.core.transport.AttributeKeys;
import io.github.lxien.orbien.core.transport.NettyConstants;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

/**
 * HTTP Keep-Alive：流关闭后保留访客 TCP，供同连接上下一个请求复用。
 */
public final class HttpKeepAliveHelper {
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(HttpKeepAliveHelper.class);
    private static final String HEADER_INJECT_HANDLER = "headerInjectDecoder";

    private HttpKeepAliveHelper() {
    }

    public static void prepareForNextRequest(Channel visitor) {
        if (visitor == null || !visitor.isActive()) {
            return;
        }
        visitor.attr(AttributeKeys.STREAM_ID).set(null);
        visitor.config().setOption(ChannelOption.AUTO_READ, true);
        visitor.read();

        ChannelPipeline pipeline = visitor.pipeline();
        if (pipeline.get(HEADER_INJECT_HANDLER) == null
                && pipeline.get(NettyConstants.HTTP_VISITOR_HANDLER) != null) {
            pipeline.addBefore(NettyConstants.HTTP_VISITOR_HANDLER, HEADER_INJECT_HANDLER, new HeaderInjectDecoder());
        }
        logger.debug("[HTTP] Keep-Alive：保留访客连接 {}，等待下一请求", visitor.id());
    }
}
