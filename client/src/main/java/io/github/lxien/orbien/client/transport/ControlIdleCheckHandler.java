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
package io.github.lxien.orbien.client.transport;

import io.github.lxien.orbien.client.statemachine.agent.AgentContext;
import io.github.lxien.orbien.client.statemachine.agent.AgentEvent;
import io.github.lxien.orbien.core.utils.ChannelUtils;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.util.concurrent.TimeUnit;

public class ControlIdleCheckHandler extends IdleStateHandler {
    private final InternalLogger logger = InternalLoggerFactory.getInstance(ControlIdleCheckHandler.class);
    private static final int MAX_MISSED = 3;
    private final AgentContext context;

    public ControlIdleCheckHandler(AgentContext context, long readerIdleTime, long writerIdleTime, long allIdleTime,
                                   TimeUnit unit) {
        super(readerIdleTime, writerIdleTime, allIdleTime, unit);
        this.context = context;
    }

    @Override
    protected void channelIdle(ChannelHandlerContext ctx, IdleStateEvent evt) {
        if (evt.state() == IdleState.READER_IDLE) {
            int missed = context.getMissedHeartbeats().incrementAndGet();
            if (missed >= MAX_MISSED) {
                logger.warn("客户端控制连接检测到连续 {} 次读超时，准备关闭并重连", missed);
                context.getMissedHeartbeats().set(0);
                ChannelUtils.closeOnFlush(ctx.channel());
                context.fireEvent(AgentEvent.DISCONNECT);
            } else {
                logger.debug("客户端控制连接读空闲第 {} 次（容忍中）", missed);
            }
        }
        ctx.fireUserEventTriggered(evt);
    }
}