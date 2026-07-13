package io.github.lxien.orbien.server.transport.bridge;

import io.github.lxien.orbien.core.transport.TunnelBridge;
import io.github.lxien.orbien.server.inspector.HttpStreamCapture;
import io.github.lxien.orbien.server.inspector.InspectorProperties;
import io.github.lxien.orbien.server.metrics.MetricsCollector;
import io.github.lxien.orbien.server.statemachine.stream.StreamContext;
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
        bridge = wrapInspection(bridge, streamContext);
        return addMetricsIfNeeded(bridge, streamContext);
    }

    /**
     * 创建多路复用（Mux）隧道桥接
     */
    public static TunnelBridge buildMux(StreamContext streamContext) {
        TunnelBridge bridge = new MultiplexTunnelBridge(streamContext);
        bridge = wrapInspection(bridge, streamContext);
        return addMetricsIfNeeded(bridge, streamContext);
    }

    /**
     * 创建 UDP 多路复用隧道桥接
     */
    public static TunnelBridge buildUdpMux(StreamContext streamContext) {
        TunnelBridge bridge = new UdpMultiplexTunnelBridge(streamContext);
        return addMetricsIfNeeded(bridge, streamContext);
    }

    private static TunnelBridge wrapInspection(TunnelBridge bridge, StreamContext ctx) {
        HttpStreamCapture capture = ctx.getHttpStreamCapture();
        if (capture != null) {
            return new InspectionTunnelBridge(bridge, capture);
        }
        return bridge;
    }

    private static TunnelBridge addMetricsIfNeeded(TunnelBridge bridge, StreamContext ctx) {
        if (metricsCollector != null && ctx.getProxyId() != null) {
            return new MetricsTunnelBridge(bridge, metricsCollector, ctx.getProxyId());
        }
        return bridge;
    }
}
