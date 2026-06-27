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

import com.xiaoniucode.etp.core.domain.HealthCheckConfig;
import com.xiaoniucode.etp.core.domain.Target;
import com.xiaoniucode.etp.core.enums.ProtocolType;
import com.xiaoniucode.etp.core.message.Message;
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

    public CompletableFuture<Message.ServiceHealth> check(String proxyId, ProtocolType protocol,
                                                          Target target,
                                                          HealthCheckConfig config) {
        long startTime = System.currentTimeMillis();
        CompletableFuture<Message.ServiceHealth> future = new CompletableFuture<>();

        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(group)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, config.getTimeout() * 1000)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        if (isHttpCheck(protocol, config)) {
                            ch.pipeline().addLast(new HttpClientCodec());
                            ch.pipeline().addLast(new HttpObjectAggregator(8192));
                            ch.pipeline().addLast(new HttpHealthHandler(config, future, startTime, proxyId, target));
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

    private boolean isHttpCheck(ProtocolType protocol, HealthCheckConfig config) {
        com.xiaoniucode.etp.core.enums.HealthCheckType type = config.getType();
        return type == com.xiaoniucode.etp.core.enums.HealthCheckType.HTTP ||
                (type == com.xiaoniucode.etp.core.enums.HealthCheckType.AUTO && protocol.isHttpOrHttps());
    }

    private void completeAsDown(CompletableFuture<Message.ServiceHealth> future, String proxyId, Target target,
                                long startTime) {
        Message.ServiceHealth health = createHealth(proxyId, target, System.currentTimeMillis() - startTime);
        future.complete(health);
    }

    private Message.ServiceHealth createHealth(String proxyId, Target target, long responseTime) {
        Message.ServiceHealth.Builder h = Message.ServiceHealth.newBuilder();
        h.setProxyId(proxyId);
        h.setHost(target.getHost());
        h.setPort(target.getPort());
        h.setStatus(Message.HealthStatus.DOWN);
        h.setResponseTimeMs(responseTime);
        return h.build();
    }

    public void shutdown() {
        group.shutdownGracefully();
    }
}