package io.github.lxien.orbien.core.transport.api;
import io.github.lxien.orbien.core.enums.TransportProtocol;

import io.github.lxien.orbien.core.transport.tcp.TcpTransportConnector;
import io.github.lxien.orbien.core.transport.tcp.TcpTransportListener;
import io.github.lxien.orbien.core.transport.websocket.WebSocketTransportConnector;
import io.github.lxien.orbien.core.transport.websocket.WebSocketTransportListener;
import io.github.lxien.orbien.core.transport.quic.QuicTransportConnector;
import io.github.lxien.orbien.core.transport.quic.QuicTransportListener;

import java.util.EnumMap;
import java.util.Map;

public final class TransportRegistry {

    private static final Map<TransportProtocol, TransportConnector> CONNECTORS = new EnumMap<>(TransportProtocol.class);
    private static final Map<TransportProtocol, TransportListener> LISTENERS = new EnumMap<>(TransportProtocol.class);

    static {
        //注册连接器
        registerConnector(new TcpTransportConnector());
        registerConnector(new WebSocketTransportConnector());
        registerConnector(new QuicTransportConnector());

        //注册监听器
        registerListener(new TcpTransportListener());
        registerListener(new WebSocketTransportListener());
        registerListener(new QuicTransportListener());
    }

    private TransportRegistry() {
    }

    public static void registerConnector(TransportConnector connector) {
        CONNECTORS.put(connector.protocol(), connector);
    }

    public static void registerListener(TransportListener listener) {
        LISTENERS.put(listener.protocol(), listener);
    }

    public static TransportConnector getConnector(TransportProtocol protocol) {
        TransportConnector connector = CONNECTORS.get(protocol);
        if (connector == null) {
            throw new IllegalArgumentException("未注册传输连接器: " + protocol);
        }
        return connector;
    }

    public static TransportListener getListener(TransportProtocol protocol) {
        TransportListener listener = LISTENERS.get(protocol);
        if (listener == null) {
            throw new IllegalArgumentException("未注册传输监听器: " + protocol);
        }
        return listener;
    }
}
