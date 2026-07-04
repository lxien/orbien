package io.github.lxien.orbien.client.transport.bridge;

import io.github.lxien.orbien.client.statemachine.stream.StreamContext;
import io.github.lxien.orbien.core.transport.TunnelBridge;

public class TunnelBridgeFactory {

    public static TunnelBridge buildDirect(StreamContext ctx) {
        final TunnelBridge base = new DirectTunnelBridge(ctx);
        TunnelBridge bridge = base;
        return bridge;
    }

    public static TunnelBridge buildMux(StreamContext ctx) {
        final TunnelBridge base = new MultiplexTunnelBridge(ctx);
        TunnelBridge bridge = base;
        return bridge;
    }
}