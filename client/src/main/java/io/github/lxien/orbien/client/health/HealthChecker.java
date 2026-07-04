/*
 *
 *  *    Copyright 2026 lxien
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

package io.github.lxien.orbien.client.health;

import io.github.lxien.orbien.core.message.Message;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;

import java.util.concurrent.CompletableFuture;

public class HealthChecker {
    private final EventLoopGroup group = new NioEventLoopGroup(1);

    public CompletableFuture<ServiceHealth> check(String proxyId, Message.Target target, Message.HealthCheck healthCheck) {
        long startTime = System.currentTimeMillis();
        CompletableFuture<ServiceHealth> future = new CompletableFuture<>();

        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(group)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, healthCheck.getTimeout() * 1000)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        if (isHttpCheck(healthCheck)) {
                            ch.pipeline().addLast(new HttpClientCodec());
                            ch.pipeline().addLast(new HttpObjectAggregator(8192));
                            ch.pipeline().addLast(new HttpHealthHandler(healthCheck, future, startTime, proxyId, target));
                        } else {
                            ch.pipeline().addLast(new TcpHealthHandler(future, proxyId, target, startTime));
                        }
                    }
                });

        bootstrap.connect(target.getHost(), target.getPort())
                .addListener((ChannelFutureListener) f -> {
                    if (!f.isSuccess()) {
                        completeAsDown(future, proxyId, target, startTime);
                    }
                });

        return future;
    }

    private boolean isHttpCheck(Message.HealthCheck config) {
        return config.getType() == Message.HealthCheckType.HEALTH_CHECK_TYPE_HTTP;
    }

    private void completeAsDown(CompletableFuture<ServiceHealth> future, String proxyId, Message.Target target,
                                long startTime) {
        ServiceHealth health = createHealth(proxyId, target, System.currentTimeMillis() - startTime);
        future.complete(health);
    }

    private ServiceHealth createHealth(String proxyId, Message.Target target, long responseTime) {
        ServiceHealth.ServiceHealthBuilder h = ServiceHealth.builder();
        h.proxyId(proxyId);
        h.host(target.getHost());
        h.port(target.getPort());
        h.status(Message.HealthStatus.DOWN);
        h.responseTimeMs(responseTime);
        return h.build();
    }

    public void shutdown() {
        group.shutdownGracefully();
    }
}