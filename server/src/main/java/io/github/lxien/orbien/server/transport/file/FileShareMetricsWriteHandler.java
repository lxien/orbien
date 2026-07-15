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

import io.github.lxien.orbien.core.enums.AgentType;
import io.github.lxien.orbien.core.transport.NettyConstants;
import io.github.lxien.orbien.server.metrics.MetricsCollector;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.channel.ChannelPipeline;
import io.netty.util.AttributeKey;

final class FileShareMetricsWriteHandler extends ChannelOutboundHandlerAdapter {

    static final String NAME = "fileShareMetricsWrite";
    static final AttributeKey<String> PROXY_ID = AttributeKey.valueOf("fileShare.metrics.proxyId");
    static final AttributeKey<AgentType> AGENT_TYPE = AttributeKey.valueOf("fileShare.metrics.agentType");

    private final MetricsCollector metricsCollector;

    FileShareMetricsWriteHandler(MetricsCollector metricsCollector) {
        this.metricsCollector = metricsCollector;
    }

    static void ensureInstalled(ChannelHandlerContext ctx, MetricsCollector metricsCollector,
                                String proxyId, AgentType agentType) {
        ChannelPipeline pipeline = ctx.pipeline();
        ctx.channel().attr(PROXY_ID).set(proxyId);
        ctx.channel().attr(AGENT_TYPE).set(agentType);
        if (pipeline.get(NAME) != null) {
            return;
        }
        FileShareMetricsWriteHandler handler = new FileShareMetricsWriteHandler(metricsCollector);
        if (pipeline.get(NettyConstants.HTTP_VISITOR_HANDLER) != null) {
            pipeline.addBefore(NettyConstants.HTTP_VISITOR_HANDLER, NAME, handler);
        } else {
            pipeline.addLast(NAME, handler);
        }
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
        String proxyId = ctx.channel().attr(PROXY_ID).get();
        if (proxyId != null && msg instanceof ByteBuf buf) {
            metricsCollector.recordOutbound(proxyId, ctx.channel().attr(AGENT_TYPE).get(), buf.readableBytes());
        }
        ctx.write(msg, promise);
    }
}
