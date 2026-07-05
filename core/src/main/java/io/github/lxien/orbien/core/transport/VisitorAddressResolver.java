package io.github.lxien.orbien.core.transport;

import io.netty.channel.Channel;
import io.netty.util.NetUtil;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;

/**
 * 统一解析访客真实 IP：优先 HAProxy PROXY 解析结果，否则 fallback 到 TCP remoteAddress。
 */
public final class VisitorAddressResolver {

    private VisitorAddressResolver() {
    }

    public static String resolveIp(Channel channel) {
        if (channel == null) {
            return null;
        }
        String realIp = channel.attr(AttributeKeys.VISITOR_REAL_IP).get();
        if (realIp != null && !realIp.isBlank()) {
            return realIp;
        }
        return resolveRemoteIp(channel);
    }

    /**
     * 仅 TCP 对端地址，不读 PROXY 属性（用于校验 LB 自身 IP）
     */
    public static String resolveRemoteOnly(Channel channel) {
        return resolveRemoteIp(channel);
    }

    private static String resolveRemoteIp(Channel channel) {
        if (!(channel.remoteAddress() instanceof InetSocketAddress addr)) {
            return null;
        }
        InetAddress address = addr.getAddress();
        if (address instanceof Inet4Address) {
            return address.getHostAddress();
        }
        if (address instanceof Inet6Address) {
            String ip = NetUtil.toAddressString(address);
            if (ip.startsWith("::ffff:")) {
                return ip.substring(7);
            }
            if ("::1".equals(ip)) {
                return "127.0.0.1";
            }
            return ip;
        }
        return address.getHostAddress();
    }
}
