package io.github.lxien.orbien.server.transport.file;

import io.github.lxien.orbien.core.domain.ProxyConfigExt;
import io.github.lxien.orbien.core.transport.AttributeKeys;
import io.github.lxien.orbien.server.service.ProxyConfigService;
import io.github.lxien.orbien.server.vhost.DomainRegistry;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.AttributeKey;
import io.netty.util.ReferenceCountUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@ChannelHandler.Sharable
public class FileShareDispatchHandler extends ChannelInboundHandlerAdapter {

    private static final AttributeKey<FileShareHttpFrameBuffer> FRAME_BUFFER =
            AttributeKey.valueOf("fileShareHttpFrameBuffer");

    @Autowired
    private DomainRegistry domainRegistry;
    @Autowired
    private ProxyConfigService proxyConfigService;
    @Autowired
    private FileShareHttpHandler fileShareHttpHandler;

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        Channel visitor = ctx.channel();
        String domain = visitor.attr(AttributeKeys.VISIT_DOMAIN).get();
        if (domain == null) {
            ctx.fireChannelRead(msg);
            return;
        }
        String proxyId = domainRegistry.getProxyIdByDomain(domain);
        if (proxyId == null) {
            ctx.fireChannelRead(msg);
            return;
        }
        ProxyConfigExt ext = proxyConfigService.findById(proxyId);
        if (ext == null || !ext.getProxyConfig().isFile()) {
            ctx.fireChannelRead(msg);
            return;
        }

        if (!(msg instanceof ByteBuf incoming)) {
            ReferenceCountUtil.release(msg);
            return;
        }

        FileShareHttpFrameBuffer frameBuffer = visitor.attr(FRAME_BUFFER).get();
        if (frameBuffer == null) {
            frameBuffer = new FileShareHttpFrameBuffer();
            visitor.attr(FRAME_BUFFER).set(frameBuffer);
        }

        dispatchCompleteRequests(ctx, ext, frameBuffer, visitor, incoming);
    }

    private void dispatchCompleteRequests(ChannelHandlerContext ctx, ProxyConfigExt ext,
                                          FileShareHttpFrameBuffer frameBuffer, Channel visitor,
                                          ByteBuf incoming) {
        ByteBuf chunk = incoming;
        while (true) {
            ByteBuf complete = frameBuffer.feed(visitor, chunk);
            chunk = null;
            if (complete == null) {
                break;
            }
            if (!complete.isReadable()) {
                ReferenceCountUtil.release(complete);
                break;
            }
            fileShareHttpHandler.handle(ctx, complete, ext);
            if (!frameBuffer.hasBuffered()) {
                break;
            }
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        FileShareHttpFrameBuffer frameBuffer = ctx.channel().attr(FRAME_BUFFER).getAndSet(null);
        if (frameBuffer != null) {
            frameBuffer.discard(ctx.channel());
        }
        ctx.fireChannelInactive();
    }
}
