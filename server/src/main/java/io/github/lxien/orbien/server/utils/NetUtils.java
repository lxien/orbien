/*
 *    Copyright 2026 lxien
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package io.github.lxien.orbien.server.utils;

import io.netty.channel.Channel;
import io.netty.util.NetUtil;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;

public final class NetUtils {

    private NetUtils() {
    }

    /**
     * 获取客户端 IP（优先 IPv4）
     */
    public static String getIp(Channel channel) {
        if (!(channel.remoteAddress() instanceof InetSocketAddress addr)) {
            return null;
        }
        InetAddress address = addr.getAddress();
        // 纯 IPv4
        if (address instanceof Inet4Address) {
            return address.getHostAddress();
        }
        // IPv6
        if (address instanceof Inet6Address) {
            String ip = NetUtil.toAddressString(address);
            // IPv4-Mapped IPv6
            if (ip.startsWith("::ffff:")) {
                return ip.substring(7);
            }
            // localhost
            if ("::1".equals(ip)) {
                return "127.0.0.1";
            }
            return ip;
        }

        return address.getHostAddress();
    }
}