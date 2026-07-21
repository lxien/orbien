package io.github.lxien.orbien.core.codec;

import io.github.lxien.orbien.core.enums.TransportProtocol;
import io.netty.buffer.ByteBuf;
import io.netty.util.NetUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class NewStreamCodec {

    public static void encode(ByteBuf buffer, String localIp, int localPort, TransportProtocol protocol) {
        buffer.writeInt(ipToInt(normalizeLocalIp(localIp)));
        buffer.writeShort(localPort);
        buffer.writeByte(protocol.toWire());
    }

    public static NewStreamInfo decode(ByteBuf buffer) {
        int ipInt = buffer.readInt();
        String localIp = intToIp(ipInt);
        int localPort = buffer.readUnsignedShort();
        return new NewStreamInfo(localIp, localPort, TransportProtocol.fromWire(buffer.readByte()));
    }

    /**
     * 协议仅支持 IPv4，将 localhost / IPv6 loopback 归一为 127.0.0.1
     */
    public static String normalizeLocalIp(String ip) {
        if (ip == null || ip.isEmpty()) {
            return ip;
        }
        String host = ip.trim();
        if ("localhost".equalsIgnoreCase(host)) {
            return "127.0.0.1";
        }
        if (NetUtil.isValidIpV6Address(host)) {
            try {
                InetAddress address = InetAddress.getByName(host);
                if (address.isLoopbackAddress()) {
                    return "127.0.0.1";
                }
            } catch (UnknownHostException ignored) {

            }
        }
        return host;
    }

    private static int ipToInt(String ip) {
        String[] parts = ip.split("\\.");
        return (Integer.parseInt(parts[0]) << 24) |
                (Integer.parseInt(parts[1]) << 16) |
                (Integer.parseInt(parts[2]) << 8) |
                Integer.parseInt(parts[3]);
    }

    private static String intToIp(int ip) {
        return String.format("%d.%d.%d.%d",
                (ip >> 24) & 0xFF,
                (ip >> 16) & 0xFF,
                (ip >> 8) & 0xFF,
                ip & 0xFF);
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