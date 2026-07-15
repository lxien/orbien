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
package io.github.lxien.orbien.server.transport.file;

import io.github.lxien.orbien.core.domain.FileShareLimitsConfig;
import io.github.lxien.orbien.core.domain.ProxyConfig;
import io.github.lxien.orbien.core.domain.ProxyConfigExt;
import io.github.lxien.orbien.core.transport.AttributeKeys;
import io.github.lxien.orbien.core.utils.ChannelUtils;
import io.github.lxien.orbien.server.metrics.MetricsCollector;
import io.github.lxien.orbien.server.service.ProxyConfigService;
import io.github.lxien.orbien.server.utils.NettyHttpUtils;
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
    @Autowired
    private MetricsCollector metricsCollector;

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

        ProxyConfig config = ext.getProxyConfig();
        FileShareMetricsWriteHandler.ensureInstalled(ctx, metricsCollector, proxyId, config.getAgentType());

        FileShareHttpFrameBuffer frameBuffer = visitor.attr(FRAME_BUFFER).get();
        if (frameBuffer == null) {
            frameBuffer = new FileShareHttpFrameBuffer(resolveMaxRequestBytes(config));
            visitor.attr(FRAME_BUFFER).set(frameBuffer);
        }

        dispatchCompleteRequests(ctx, ext, frameBuffer, visitor, incoming);
    }

    private void dispatchCompleteRequests(ChannelHandlerContext ctx, ProxyConfigExt ext,
                                          FileShareHttpFrameBuffer frameBuffer, Channel visitor,
                                          ByteBuf incoming) {
        ProxyConfig config = ext.getProxyConfig();
        ByteBuf chunk = incoming;
        while (true) {
            ByteBuf complete = frameBuffer.feed(visitor, chunk);
            chunk = null;
            if (frameBuffer.isOverflow()) {
                visitor.attr(FRAME_BUFFER).set(null);
                NettyHttpUtils.sendHttp413(visitor).addListener(f -> ChannelUtils.closeOnFlush(visitor));
                return;
            }
            if (complete == null) {
                break;
            }
            if (!complete.isReadable()) {
                ReferenceCountUtil.release(complete);
                break;
            }
            metricsCollector.recordInbound(config.getProxyId(), config.getAgentType(), complete.readableBytes());
            fileShareHttpHandler.handle(ctx, complete, ext);
            if (!frameBuffer.hasBuffered()) {
                break;
            }
        }
    }

    private static long resolveMaxRequestBytes(ProxyConfig config) {
        long max = FileShareLimitsConfig.DEFAULT_MAX_UPLOAD_SIZE;
        if (config.hasFileShareLimits() && config.getFileShareLimits().getMaxUploadSize() != null) {
            max = config.getFileShareLimits().getMaxUploadSize();
        }
        return Math.max(FileShareHttpFrameBuffer.MAX_HEADER_BYTES, max);
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
