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

package io.github.lxien.orbien.server.transport.socks5;

import io.github.lxien.orbien.core.transport.TunnelEntry;
import io.github.lxien.orbien.core.utils.ChannelUtils;
import io.github.lxien.orbien.server.statemachine.stream.StreamContext;
import io.github.lxien.orbien.server.statemachine.stream.StreamEvent;
import io.github.lxien.orbien.server.statemachine.stream.StreamManager;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * SOCKS5 握手完成后的双向数据中继。
 */
@Component
@ChannelHandler.Sharable
public class Socks5RelayHandler extends SimpleChannelInboundHandler<ByteBuf> {

    private final InternalLogger logger = InternalLoggerFactory.getInstance(Socks5RelayHandler.class);

    @Autowired
    private StreamManager streamManager;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) {
        Channel visitor = ctx.channel();
        Optional<StreamContext> contextOpt = streamManager.getStreamContext(visitor);
        if (contextOpt.isEmpty()) {
            logger.debug("[SOCKS5] 未找到流上下文，关闭连接");
            ChannelUtils.closeOnFlush(visitor);
            return;
        }
        StreamContext streamContext = contextOpt.get();
        if (!streamContext.canAcceptVisitorData()) {
            return;
        }
        TunnelEntry tunnelEntry = streamContext.getTunnelEntry();
        if (tunnelEntry == null || streamContext.getTunnelBridge() == null) {
            return;
        }
        Channel tunnel = tunnelEntry.getChannel();
        if (!tunnel.isWritable()) {
            streamContext.pauseVisitorRead(StreamContext.VISITOR_PAUSE_BACKPRESSURE);
            streamManager.addPausedStreamId(tunnel, streamContext.getStreamId());
        }
        streamContext.forwardToLocal(msg);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        logger.error("[SOCKS5] 流异常", cause);
        ctx.fireExceptionCaught(cause);
    }

    @Override
    public void channelWritabilityChanged(ChannelHandlerContext ctx) {
        streamManager.getStreamContext(ctx.channel()).ifPresent(context ->
                context.onVisitorWritabilityChanged(ctx.channel().isWritable()));
        ctx.fireChannelWritabilityChanged();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        streamManager.getStreamContext(ctx.channel()).ifPresent(context ->
                context.fireEvent(StreamEvent.STREAM_LOCAL_CLOSE));
        ctx.fireChannelInactive();
    }
}
