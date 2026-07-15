package io.github.lxien.orbien.core.transport;

import io.github.lxien.orbien.core.enums.ProtocolType;
import io.github.lxien.orbien.core.enums.TransportProtocol;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.util.AttributeKey;

public class AttributeKeys {
    public static final AttributeKey<Integer> CONNECTION_ID = AttributeKey.valueOf("orbien.connection_id");
    public static final AttributeKey<Integer> STREAM_ID = AttributeKey.valueOf("orbien.streamId");
    public static final AttributeKey<String> BASIC_AUTH_HEADER = AttributeKey.valueOf("orbien.token");
    public static final AttributeKey<Boolean> BASIC_AUTH_PASSED = AttributeKey.valueOf("orbien.basicAuth.passed");
    public static final AttributeKey<Boolean> IP_ACCESS_PASSED = AttributeKey.valueOf("orbien.ipAccess.passed");
    public static final AttributeKey<ProtocolType> PROTOCOL_TYPE = AttributeKey.valueOf("orbien.protocol_type");
    public static final AttributeKey<String> VISIT_DOMAIN = AttributeKey.valueOf("orbien.visitorDomain");
    public static final AttributeKey<String> VISITOR_REAL_IP = AttributeKey.valueOf("orbien.visitorRealIp");
    public static final AttributeKey<ByteBuf> HTTP_FIRST_PACKET = AttributeKey.valueOf("cachedFirstPacket");
    public static final AttributeKey<ByteBuf> PENDING_READ = AttributeKey.valueOf("pending.read");
    public static final AttributeKey<ChannelType> CHANNEL_TYPE = AttributeKey.valueOf("channel.type");
    public static final AttributeKey<TransportProtocol> TRANSPORT_PROTOCOL = AttributeKey.valueOf("transport.protocol");
    /**
     * QUIC 连接级 Channel（QuicChannel），用于保活与诊断
     */
    public static final AttributeKey<Channel> QUIC_CONNECTION = AttributeKey.valueOf("quic.connection");
    /**
     * QUIC 底层 UDP Channel
     */
    public static final AttributeKey<Channel> QUIC_DATAGRAM = AttributeKey.valueOf("quic.datagram");
    /**
     * 独立隧道是否处于原始 TCP 透传模式
     */
    public static final AttributeKey<Boolean> DIRECT_PASSTHROUGH = AttributeKey.valueOf("direct.passthrough");
}
