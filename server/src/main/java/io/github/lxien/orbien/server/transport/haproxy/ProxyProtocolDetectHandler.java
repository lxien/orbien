package io.github.lxien.orbien.server.transport.haproxy;

import io.github.lxien.orbien.core.transport.NettyConstants;
import io.github.lxien.orbien.server.config.domain.ProxyProtocolConfig;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.ProtocolDetectionResult;
import io.netty.handler.codec.ProtocolDetectionState;
import io.netty.handler.codec.haproxy.HAProxyMessageDecoder;
import io.netty.handler.codec.haproxy.HAProxyProtocolVersion;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.util.List;

/**
 * 首包探测 HA PROXY v1/v2 获取真实来源IP地址
 * @author lxien
 */
public class ProxyProtocolDetectHandler extends ByteToMessageDecoder {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(ProxyProtocolDetectHandler.class);

    private final ProxyProtocolConfig config;

    public ProxyProtocolDetectHandler(ProxyProtocolConfig config) {
        this.config = config;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        ProtocolDetectionResult<HAProxyProtocolVersion> result = HAProxyMessageDecoder.detectProtocol(in);
        if (result.state() == ProtocolDetectionState.NEEDS_MORE_DATA) {
            return;
        }

        ChannelPipeline pipeline = ctx.pipeline();
        if (result.state() == ProtocolDetectionState.INVALID) {
            if (config.isStrict()) {
                logger.warn("[HAProxy] strict 模式拒绝无 PROXY 头的连接 peer={}", ctx.channel().remoteAddress());
                ctx.close();
                return;
            }
            // 直连调试：去掉探测 handler，原样转发 HTTP/TCP
            pipeline.remove(this);
            out.add(in.readRetainedSlice(in.readableBytes()));
            logger.debug("[HAProxy] 非 PROXY 连接，按直连处理 peer={}", ctx.channel().remoteAddress());
            return;
        }

        // 检测到 PROXY 头，动态挂载解码链
        pipeline.addAfter(ctx.name(), NettyConstants.HAPROXY_DECODER, new HAProxyMessageDecoder());
        pipeline.addAfter(NettyConstants.HAPROXY_DECODER, NettyConstants.HAPROXY_ADDRESS_HANDLER,
                new HAProxyVisitorAddressHandler(config));
        pipeline.remove(this);
        ctx.fireChannelRead(in.retain());
    }
}
