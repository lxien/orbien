package io.github.lxien.orbien.core.codec;

import io.github.lxien.orbien.core.enums.TransportProtocol;
import io.netty.buffer.ByteBuf;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.nio.charset.StandardCharsets;

public class NewStreamCodec {

    public static void encode(ByteBuf buffer, String localIp, int localPort, TransportProtocol protocol) {
        String host = normalizeLocalIp(localIp);
        if (host == null || host.isEmpty()) {
            throw new IllegalArgumentException("目标地址不能为空");
        }
        byte[] hostBytes = host.getBytes(StandardCharsets.UTF_8);
        if (hostBytes.length > 0xFFFF) {
            throw new IllegalArgumentException("目标地址过长: " + host);
        }
        buffer.writeShort(hostBytes.length);
        buffer.writeBytes(hostBytes);
        buffer.writeShort(localPort);
        buffer.writeByte(protocol.toWire());
    }

    public static NewStreamInfo decode(ByteBuf buffer) {
        int hostLen = buffer.readUnsignedShort();
        if (hostLen == 0 || buffer.readableBytes() < hostLen + 3) {
            throw new IllegalArgumentException("目标地址不完整");
        }
        String localIp = buffer.readCharSequence(hostLen, StandardCharsets.UTF_8).toString();
        int localPort = buffer.readUnsignedShort();
        return new NewStreamInfo(localIp, localPort, TransportProtocol.fromWire(buffer.readByte()));
    }
    public static String normalizeLocalIp(String ip) {
        if (ip == null) {
            return null;
        }
        return ip.trim();
    }

    @AllArgsConstructor
    @Getter
    @Setter
    public static class NewStreamInfo {
        private final String localIp;
        private final int localPort;
        private final TransportProtocol transportProtocol;
    }
}
