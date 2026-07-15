package io.github.lxien.orbien.server.transport.bridge;

import io.github.lxien.orbien.core.transport.TunnelBridge;
import io.github.lxien.orbien.server.inspector.HttpStreamCapture;
import io.github.lxien.orbien.server.metrics.MetricsCollector;
import io.github.lxien.orbien.server.service.ProxyConfigService;
import io.github.lxien.orbien.server.statemachine.stream.StreamContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class TunnelBridgeFactory {
    private static MetricsCollector metricsCollector;
    private static ProxyConfigService proxyConfigService;

    @Autowired
    public void setMetricsCollector(MetricsCollector collector) {
        TunnelBridgeFactory.metricsCollector = collector;
    }

    @Autowired
    public void setProxyConfigService(ProxyConfigService service) {
        TunnelBridgeFactory.proxyConfigService = service;
    }

    public static TunnelBridge buildDirect(StreamContext streamContext) {
        TunnelBridge bridge = new DirectTunnelBridge(streamContext);
        bridge = wrapHeaderRewrite(bridge, streamContext);
        bridge = wrapInspection(bridge, streamContext);
        return addMetricsIfNeeded(bridge, streamContext);
    }

    public static TunnelBridge buildMux(StreamContext streamContext) {
        TunnelBridge bridge = new MultiplexTunnelBridge(streamContext);
        bridge = wrapHeaderRewrite(bridge, streamContext);
        bridge = wrapInspection(bridge, streamContext);
        return addMetricsIfNeeded(bridge, streamContext);
    }

    public static TunnelBridge buildUdpMux(StreamContext streamContext) {
        TunnelBridge bridge = new UdpMultiplexTunnelBridge(streamContext);
        return addMetricsIfNeeded(bridge, streamContext);
    }

    private static TunnelBridge wrapHeaderRewrite(TunnelBridge bridge, StreamContext ctx) {
        return HeaderRewriteResponseBridge.wrapIfNeeded(bridge, ctx, proxyConfigService);
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
