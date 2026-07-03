package com.xiaoniucode.etp.server.transport.bridge;

import com.xiaoniucode.etp.core.transport.TunnelBridge;
import com.xiaoniucode.etp.server.metrics.MetricsCollector;
import com.xiaoniucode.etp.server.statemachine.stream.StreamContext;
import com.xiaoniucode.etp.server.transport.udp.UdpMultiplexTunnelBridge;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * TunnelBridge 工厂类
 * 负责创建 Direct / Mux 类型的隧道桥接，并统一添加流量统计装饰器
 */
@Component
public class TunnelBridgeFactory {

    private static MetricsCollector metricsCollector;

    @Autowired
    public void setMetricsCollector(MetricsCollector collector) {
        TunnelBridgeFactory.metricsCollector = collector;
    }

    /**
     * 创建直接（Direct）隧道桥接
     */
    public static TunnelBridge buildDirect(StreamContext streamContext) {
        TunnelBridge bridge = new DirectTunnelBridge(streamContext);
        TunnelBridge tunnelBridge = addMetricsIfNeeded(bridge, streamContext);
        return new BackpressureTunnelBridge(tunnelBridge, streamContext);
    }

    /**
     * 创建多路复用（Mux）隧道桥接
     */
    public static TunnelBridge buildMux(StreamContext streamContext) {
        TunnelBridge bridge = new MultiplexTunnelBridge(streamContext);
        TunnelBridge tunnelBridge = addMetricsIfNeeded(bridge, streamContext);
        return new BackpressureTunnelBridge(tunnelBridge, streamContext);
    }

    /**
     * 创建 UDP 多路复用隧道桥接
     */
    public static TunnelBridge buildUdpMux(StreamContext streamContext) {
        TunnelBridge bridge = new UdpMultiplexTunnelBridge(streamContext);
        TunnelBridge tunnelBridge = addMetricsIfNeeded(bridge, streamContext);
        return new BackpressureTunnelBridge(tunnelBridge, streamContext);
    }

    /**
     * 如果配置了 MetricsCollector 且存在 proxyId，则包装上流量统计装饰器
     */
    private static TunnelBridge addMetricsIfNeeded(TunnelBridge bridge, StreamContext ctx) {
        if (metricsCollector != null && ctx.getProxyId() != null) {
            return new MetricsTunnelBridge(bridge, metricsCollector, ctx.getProxyId());
        }
        return bridge;
    }
}