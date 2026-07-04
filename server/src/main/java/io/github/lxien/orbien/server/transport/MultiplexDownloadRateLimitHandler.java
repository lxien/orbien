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

package io.github.lxien.orbien.server.transport;

import io.github.lxien.orbien.core.message.TMSP;
import io.github.lxien.orbien.core.message.TMSPFrame;
import io.github.lxien.orbien.core.transport.TunnelEntry;
import io.github.lxien.orbien.server.statemachine.stream.StreamContext;
import io.github.lxien.orbien.server.statemachine.stream.StreamEvent;
import io.github.lxien.orbien.server.statemachine.stream.StreamManager;
import io.github.lxien.orbien.server.statemachine.stream.StreamState;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.concurrent.TimeUnit;

@Component
@ChannelHandler.Sharable
public class MultiplexDownloadRateLimitHandler extends SimpleChannelInboundHandler<TMSPFrame> {
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(MultiplexDownloadRateLimitHandler.class);
    @Autowired
    private StreamManager streamManager;
    private static final long MAX_WAIT_MS = 500;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, TMSPFrame frame) {
        ByteBuf payload = frame.getPayload();
        ReferenceCountUtil.retain(frame);
        if (payload == null || !payload.isReadable()) {
            ctx.fireChannelRead(frame);
            return;
        }
        if (frame.getMsgType() != TMSP.MSG_STREAM_DATA) {
            ctx.fireChannelRead(frame);
            return;
        }
        StreamContext streamContext = streamManager.getStreamContext(frame.getStreamId());
        if (streamContext == null || streamContext.getState() != StreamState.OPENED) {
            ctx.fireChannelRead(frame);
            return;
        }
        BandwidthLimiter limiter = streamContext.getBandwidthLimiter();
        if (limiter == null) {
            ctx.fireChannelRead(frame);
            return;
        }
        int bytes = payload.readableBytes();
        long waitNanos = limiter.getUploadWaitNanos(bytes);
        long waitMillis = Math.max(1, waitNanos / 1_000_000);
        logger.warn("访问速度太快，触发限流：streamId={} bytes={} 等待 {} ms", streamContext.getStreamId(), bytes, waitNanos / 1_000_000);
        Channel visitor = streamContext.getVisitor();
        ArrayDeque<TMSPFrame> messagesQueue = streamContext.getMessagesQueue();
        messagesQueue.add(frame);
        if (waitMillis > MAX_WAIT_MS || messagesQueue.size() > 1000) {
            streamContext.fireEvent(StreamEvent.STREAM_LOCAL_PAUSE);
            scheduleResume(ctx,streamContext, waitNanos);
        } else {
            logger.debug("恢复数据");
            while (!messagesQueue.isEmpty() && visitor.isWritable()) {
                ctx.fireChannelRead(messagesQueue.pop());
            }
        }
    }

    private void scheduleResume(ChannelHandlerContext ctx, StreamContext streamContext, long waitNanos) {
        TunnelEntry tunnelEntry = streamContext.getTunnelEntry();
        Channel visitor = streamContext.getVisitor();
        Channel tunnel = tunnelEntry.getChannel();
        if (tunnel == null) return;
        long waitMillis = Math.max(1, waitNanos / 1_000_000);
        tunnel.eventLoop().schedule(() -> {
            ArrayDeque<TMSPFrame> messagesQueue = streamContext.getMessagesQueue();
            logger.debug("恢复数据");
            while (!messagesQueue.isEmpty() && visitor.isWritable()) {
                ctx.fireChannelRead(messagesQueue.pop());
            }
            streamContext.fireEvent(StreamEvent.STREAM_LOCAL_RESUME);
            logger.debug("限流恢复，继续读取：streamId={}", streamContext.getStreamId());
        }, waitMillis, TimeUnit.MILLISECONDS);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        logger.error("MultiplexDownloadRateLimitHandler 异常", cause);
        ctx.fireExceptionCaught(cause);
    }
}
