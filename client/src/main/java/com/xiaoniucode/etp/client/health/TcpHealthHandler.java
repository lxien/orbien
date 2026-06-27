/*
 *
 *  *    Copyright 2026 xiaoniucode
 *  *
 *  *    Licensed under the Apache License, Version 2.0 (the "License");
 *  *    you may not use this file except in compliance with the License.
 *  *    You may obtain a copy of the License at
 *  *
 *  *        http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  *    Unless required by applicable law or agreed to in writing, software
 *  *    distributed under the License is distributed on an "AS IS" BASIS,
 *  *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  *    See the License for the specific language governing permissions and
 *  *    limitations under the License.
 *
 */

package com.xiaoniucode.etp.client.health;

import com.xiaoniucode.etp.core.domain.Target;
import com.xiaoniucode.etp.core.message.Message;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.util.concurrent.CompletableFuture;


public class TcpHealthHandler extends ChannelInboundHandlerAdapter {
    private final CompletableFuture<Message.ServiceHealth> future;
    private final long startTime;
    private final String proxyId;
    private final Target target;

    public TcpHealthHandler(CompletableFuture<Message.ServiceHealth> future, String proxyId, Target target, long startTime) {
        this.future = future;
        this.startTime = startTime;
        this.proxyId = proxyId;
        this.target = target;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        ctx.close();
        // TCP 连接成功 = UP
        Message.ServiceHealth health = createHealth(System.currentTimeMillis() - startTime);
        future.complete(health);
    }

    private Message.ServiceHealth createHealth(long responseTime) {
        Message.ServiceHealth.Builder health = Message.ServiceHealth.newBuilder();
        health.setProxyId(proxyId);
        health.setHost(target.getHost());
        health.setPort(target.getPort());
        health.setStatus(Message.HealthStatus.UP);
        health.setResponseTimeMs(responseTime);
        return health.build();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        ctx.close();
        future.completeExceptionally(cause);
    }
}