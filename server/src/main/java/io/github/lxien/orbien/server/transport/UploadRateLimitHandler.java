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

import io.github.lxien.orbien.core.enums.ProtocolType;
import io.github.lxien.orbien.core.transport.AttributeKeys;
import io.github.lxien.orbien.core.utils.ChannelUtils;
import io.github.lxien.orbien.server.statemachine.stream.StreamContext;
import io.github.lxien.orbien.server.statemachine.stream.StreamManager;
import io.github.lxien.orbien.server.utils.NettyHttpUtils;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Component
@ChannelHandler.Sharable
public class UploadRateLimitHandler extends SimpleChannelInboundHandler<ByteBuf> {
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(UploadRateLimitHandler.class);
    @Autowired
    private StreamManager streamManager;
    /**
     * 限流最大等待时间，如果超过这个时间直接决绝
     */
    private static final long MAX_WAIT_MS = 500;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf payload) {
        if (!payload.isReadable()) {
            return;
        }
        logger.debug("进入上传限流处理器");
        Channel visitor = ctx.channel();
        Optional<StreamContext> streamContextOpt = streamManager.getStreamContext(visitor);
        if (streamContextOpt.isEmpty()) {
            ctx.fireChannelRead(payload.retain());
            return;
        }
        StreamContext streamContext = streamContextOpt.get();
        BandwidthLimiter limiter = streamContext.getBandwidthLimiter();
        if (limiter == null) {
            logger.debug("没有配置限速");
            ctx.fireChannelRead(payload.retain());
            return;
        }
        if (limiter.tryUpload(payload)) {
            logger.debug("访问速度正常，继续转发：streamId={}", streamContext.getStreamId());
            ctx.fireChannelRead(payload.retain());
            return;
        }
        int bytes = payload.readableBytes();
        long waitNanos = limiter.getUploadWaitNanos(bytes);
        long waitMillis = Math.max(1, waitNanos / 1_000_000);

        if (waitMillis > MAX_WAIT_MS) {
            logger.debug("访问速度过快，触发强限流（直接拒绝）：streamId={} wait={}ms", streamContext.getStreamId(), waitMillis);
            ProtocolType protocol = streamContext.getProtocol();
            if (protocol.isHttp()) {
                //响应HTTP 上传时发 429
                NettyHttpUtils.sendHttpTooManyRequests(visitor)
                        .addListener(f -> {
                            ChannelUtils.closeOnFlush(visitor);
                        });
            }
            if (protocol.isTcp()) {
                ChannelUtils.closeOnFlush(visitor);
            }
            return;
        }

        logger.debug("访问速度太快，触发限流：streamId={} bytes={} 等待 {} ms", streamContext.getStreamId(), bytes, waitNanos / 1_000_000);
        visitor.config().setOption(ChannelOption.AUTO_READ, false);
        visitor.attr(AttributeKeys.PENDING_READ).set(payload.retain());
        visitor.eventLoop().schedule(() -> {
            ctx.fireChannelRead(visitor.attr(AttributeKeys.PENDING_READ).get());
            visitor.config().setOption(ChannelOption.AUTO_READ, true);
            visitor.read();
            logger.debug("限流恢复，继续读取：streamId={}", streamContext.getStreamId());
        }, waitMillis, TimeUnit.MILLISECONDS);
        logger.debug("发送限流时从访问流收到的数据包到内网");
    }
}
