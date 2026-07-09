package io.github.lxien.orbien.server.web.service.support;

import io.github.lxien.orbien.core.domain.transport.QuicProtocolConfig;
import io.github.lxien.orbien.core.domain.transport.WebSocketProtocolConfig;
import io.github.lxien.orbien.core.enums.TransportProtocol;
import io.github.lxien.orbien.server.config.AppConfig;
import io.github.lxien.orbien.server.config.domain.TransportConfig;
import io.github.lxien.orbien.server.web.common.exception.BizException;
import io.github.lxien.orbien.server.web.dto.transport.TransportProtocolConstraints;

import java.util.ArrayList;
import java.util.List;

/**
 * 数据隧道传输协议可选项与校验。
 */
public final class TransportProtocolConstraintSupport {

    private TransportProtocolConstraintSupport() {
    }

    public static TransportProtocolConstraints build(AppConfig appConfig) {
        TransportProtocolConstraints constraints = new TransportProtocolConstraints();
        List<Integer> available = new ArrayList<>();
        available.add(TransportProtocol.TCP.getCode());
        constraints.setTcpPort(appConfig.getServerPort());

        TransportConfig transportConfig = appConfig.getTransportConfig();
        if (transportConfig != null) {
            WebSocketProtocolConfig websocket = transportConfig.getWebsocket();
            if (websocket != null && websocket.isEnabled()) {
                available.add(TransportProtocol.WEBSOCKET.getCode());
                constraints.setWebsocketEnabled(true);
                constraints.setWebsocketPort(websocket.getPort());
            } else {
                constraints.setWebsocketEnabled(false);
            }
            QuicProtocolConfig quic = transportConfig.getQuic();
            if (quic != null && quic.isEnabled()) {
                available.add(TransportProtocol.QUIC.getCode());
                constraints.setQuicEnabled(true);
                constraints.setQuicPort(quic.getPort());
            } else {
                constraints.setQuicEnabled(false);
            }
        }
        constraints.setAvailableProtocols(available);
        return constraints;
    }

    public static void validateAvailable(AppConfig appConfig, TransportProtocol protocol) {
        if (protocol == null) {
            throw new BizException("传输协议无效");
        }
        if (protocol == TransportProtocol.TCP) {
            return;
        }
        TransportConfig transportConfig = appConfig.getTransportConfig();
        if (transportConfig == null) {
            throw new BizException("服务端未启用 " + protocol.getName() + " 传输");
        }
        if (protocol == TransportProtocol.WEBSOCKET) {
            WebSocketProtocolConfig websocket = transportConfig.getWebsocket();
            if (websocket == null || !websocket.isEnabled()) {
                throw new BizException("服务端未启用 WebSocket 传输，请配置 transport.websocket");
            }
            return;
        }
        if (protocol == TransportProtocol.QUIC) {
            QuicProtocolConfig quic = transportConfig.getQuic();
            if (quic == null || !quic.isEnabled()) {
                throw new BizException("服务端未启用 QUIC 传输");
            }
        }
    }

    public static TransportProtocol resolveStoredProtocol(TransportProtocol stored) {
        return stored != null ? stored : TransportProtocol.TCP;
    }
}
