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

package io.github.lxien.orbien.server.transport.bridge;

import io.github.lxien.orbien.core.transport.TunnelBridge;
import io.github.lxien.orbien.server.metrics.MetricsCollector;
import io.netty.buffer.ByteBuf;
import io.netty.util.concurrent.Future;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

/**
 * 流量统计装饰器，用于统计隧道的流量
 */
public class MetricsTunnelBridge implements TunnelBridge {
    private final InternalLogger logger = InternalLoggerFactory.getInstance(MetricsTunnelBridge.class);
    private final TunnelBridge delegate;
    private final MetricsCollector metricsCollector;
    private final String proxyId;

    public MetricsTunnelBridge(TunnelBridge delegate, MetricsCollector metricsCollector, String proxyId) {
        this.delegate = delegate;
        this.metricsCollector = metricsCollector;
        this.proxyId = proxyId;
        logger.debug("为代理ID: {} 创建流量统计装饰器", proxyId);
    }

    @Override
    public Future<Void> openAsync() {
        return delegate.openAsync();
    }

    @Override
    public void forwardToLocal(ByteBuf payload) {
        forwardToLocal(payload, true);
    }

    @Override
    public void forwardToLocal(ByteBuf payload, boolean sharedWithInbound) {
        int bytes = payload.readableBytes();
        if (bytes > 0) {
            logger.debug("流量统计: proxyId-{} 向本地转发 {} 字节数据", proxyId, bytes);
            metricsCollector.collect(proxyId, collector -> {
                collector.incReadBytes(bytes);
                collector.incReadMessages(1);
            });
        }
        delegate.forwardToLocal(payload, sharedWithInbound);
    }

    @Override
    public void forwardToRemote(ByteBuf payload) {
        forwardToRemote(payload, true);
    }

    @Override
    public void forwardToRemote(ByteBuf payload, boolean sharedWithInbound) {
        int bytes = payload.readableBytes();
        if (bytes > 0) {
            logger.debug("流量统计: proxyId-{} 向远程转发 {} 字节数据", proxyId, bytes);
            metricsCollector.collect(proxyId, metrics -> {
                metrics.incWriteBytes(bytes);
                metrics.incWriteMessages(1);
            });
        }
        delegate.forwardToRemote(payload, sharedWithInbound);
    }
}
