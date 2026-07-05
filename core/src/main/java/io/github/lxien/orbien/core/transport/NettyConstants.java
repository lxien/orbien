package io.github.lxien.orbien.core.transport;

public interface NettyConstants {
    String TMSP_CODEC = "TMSPCodec";
    String CONTROL_FRAME_HANDLER = "controlFrameHandler";
    String IDLE_CHECK_HANDLER = "idleCheckHandler";
    String CONTROL_IDLE_CHECK_HANDLER = "controlidlecheckhandler";
    String REAL_SERVER_HANDLER = "realServerHandler";
    String UDP_REAL_SERVER_HANDLER = "udpRealServerHandler";
    String TLS_HANDLER = "tlsHandler";
    String TCP_VISITOR_HANDLER = "tcpVisitorHandler";
    String UDP_VISITOR_HANDLER = "udpVisitorHandler";
    String HTTP_VISITOR_HANDLER = "httpVisitorHandler";
    String HAPROXY_DETECT_HANDLER = "haproxyDetectHandler";
    String HAPROXY_DECODER = "haproxyDecoder";
    String HAPROXY_ADDRESS_HANDLER = "haproxyAddressHandler";
    String DIRECT_TUNNEL_BRIDGE_HANDLER = "directTunnelBridgeBandler";
    String WEBSOCKET_HANDLER = "webSocketHandler";
    String WEBSOCKET_FRAME_CODEC = "webSocketFrameCodec";
    String SNAPPY_ENCODER = "snappyEncoder";
    String SNAPPY_DECODER = "snappyDecoder";
    String DOWNLOAD_RATE_LIMIT_HANDLER = "downloadRateLimitHandler";

}
