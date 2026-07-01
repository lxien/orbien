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

import com.xiaoniucode.etp.core.message.Message;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;

import java.util.concurrent.CompletableFuture;

public class HttpHealthHandler extends SimpleChannelInboundHandler<FullHttpResponse> {
    private final Message.HealthCheck healthCheck;
    private final CompletableFuture<ServiceHealth> resultFuture;
    private final long startTime;
    private final String proxyId;
    private final Message.Target target;

    public HttpHealthHandler(Message.HealthCheck healthCheck, CompletableFuture<ServiceHealth> resultFuture,
                             long startTime, String proxyId, Message.Target target) {
        this.healthCheck = healthCheck;
        this.resultFuture = resultFuture;
        this.startTime = startTime;
        this.proxyId = proxyId;
        this.target = target;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        FullHttpRequest request = new DefaultFullHttpRequest(
                HttpVersion.HTTP_1_1,
                HttpMethod.GET,
                healthCheck.getPath()
        );

        request.headers()
                .set(HttpHeaderNames.HOST, target.getHost() + ":" + target.getPort())
                .set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE)
                .set(HttpHeaderNames.USER_AGENT, "ETP-HealthChecker");

        ctx.writeAndFlush(request);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpResponse response) {
        long responseTime = System.currentTimeMillis() - startTime;
        boolean isHealthy = false;
        String errorMsg = null;

        try {
            int statusCode = response.status().code();
            isHealthy = statusCode == 200;
            if (!isHealthy) {
                errorMsg = "Unexpected status code: " + statusCode;
            }
        } catch (Exception e) {
            errorMsg = "Response parse error: " + e.getMessage();
        }

        ServiceHealth health = buildServiceHealth(isHealthy, responseTime, errorMsg);
        resultFuture.complete(health);
        ctx.close();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        long responseTime = System.currentTimeMillis() - startTime;
        ServiceHealth health = buildServiceHealth(false, responseTime, cause.getMessage());
        resultFuture.complete(health);
        ctx.close();
    }

    private ServiceHealth buildServiceHealth(boolean isHealthy, long responseTime, String errorMsg) {
        ServiceHealth.ServiceHealthBuilder health = ServiceHealth.builder();
        health.proxyId(proxyId);
        health.host(target.getHost());
        health.port(target.getPort());
        health.status(isHealthy ? Message.HealthStatus.UP : Message.HealthStatus.DOWN);
        health.responseTimeMs(responseTime);
        return health.build();
    }
}