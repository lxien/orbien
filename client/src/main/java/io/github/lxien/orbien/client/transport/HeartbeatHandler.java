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

import io.github.lxien.orbien.core.message.TMSP;
import io.github.lxien.orbien.core.message.TMSPFrame;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.util.concurrent.TimeUnit;

import io.netty.util.concurrent.ScheduledFuture;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

/**
 * 心跳任务，用于定时向服务端发送心跳消息
 */
public class HeartbeatHandler extends ChannelInboundHandlerAdapter {
private final InternalLogger logger= InternalLoggerFactory.getInstance(HeartbeatHandler.class);
    private final long heartbeatIntervalSeconds;
    /**
     * 心跳任务句柄
     */
    private volatile ScheduledFuture<?> heartbeatFuture;

    public HeartbeatHandler(long heartbeatIntervalSeconds) {
        this.heartbeatIntervalSeconds = heartbeatIntervalSeconds;
        logger.debug("心跳处理器初始化，心跳间隔: {}秒", heartbeatIntervalSeconds);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        logger.debug("连接激活，开始启动心跳任务");
        startHeartbeat(ctx);
        ctx.fireChannelActive();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        logger.debug("连接断开，停止心跳任务");
        stopHeartbeat();
        ctx.fireChannelInactive();
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) {
        logger.debug("处理器被移除，停止心跳任务");
        stopHeartbeat();
    }

    /**
     * 启动心跳
     */
    private void startHeartbeat(ChannelHandlerContext ctx) {
        if (heartbeatFuture != null && !heartbeatFuture.isCancelled()) {
            logger.debug("心跳任务已存在，无需重复启动");
            return;
        }
        Channel channel = ctx.channel();
        logger.debug("启动心跳任务，间隔: {}秒", heartbeatIntervalSeconds);
        heartbeatFuture = ctx.executor().scheduleAtFixedRate(() -> {
            if (!channel.isActive()) {
                logger.debug("连接未激活，跳过本次心跳");
                return;
            }
            logger.debug("发送心跳消息");
            channel.writeAndFlush(new TMSPFrame(0, TMSP.MSG_PING));
        }, 0, heartbeatIntervalSeconds, TimeUnit.SECONDS);
    }

    /**
     * 停止心跳
     */
    private void stopHeartbeat() {
        if (heartbeatFuture != null) {
            logger.debug("停止心跳任务");
            heartbeatFuture.cancel(false);
            heartbeatFuture = null;
            logger.debug("心跳任务已停止");
        }
    }
}