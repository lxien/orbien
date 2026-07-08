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

package io.github.lxien.orbien.core.socks5;

import io.netty.buffer.ByteBuf;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;

/**
 * SOCKS5 请求中的目标地址。
 */
public record Socks5Address(byte addressType, String host, int port) {

    public static Socks5Address decode(ByteBuf in) {
        byte addressType = in.readByte();
        String host;
        switch (addressType) {
            case Socks5Constants.ATYP_IPV4 -> {
                byte[] bytes = new byte[4];
                in.readBytes(bytes);
                try {
                    host = InetAddress.getByAddress(bytes).getHostAddress();
                } catch (UnknownHostException e) {
                    throw new IllegalArgumentException("无效的 IPv4 地址", e);
                }
            }
            case Socks5Constants.ATYP_DOMAIN -> {
                int length = in.readUnsignedByte();
                host = in.readCharSequence(length, StandardCharsets.US_ASCII).toString();
            }
            case Socks5Constants.ATYP_IPV6 -> {
                byte[] bytes = new byte[16];
                in.readBytes(bytes);
                try {
                    host = InetAddress.getByAddress(bytes).getHostAddress();
                } catch (UnknownHostException e) {
                    throw new IllegalArgumentException("无效的 IPv6 地址", e);
                }
            }
            default -> throw new IllegalArgumentException("不支持的地址类型: " + addressType);
        }
        int port = in.readUnsignedShort();
        return new Socks5Address(addressType, host, port);
    }

    public void encode(ByteBuf out) {
        out.writeByte(addressType);
        switch (addressType) {
            case Socks5Constants.ATYP_IPV4 -> {
                try {
                    out.writeBytes(InetAddress.getByName(host).getAddress());
                } catch (UnknownHostException e) {
                    throw new IllegalArgumentException("无效的 IPv4 地址: " + host, e);
                }
            }
            case Socks5Constants.ATYP_DOMAIN -> {
                byte[] domainBytes = host.getBytes(StandardCharsets.US_ASCII);
                if (domainBytes.length > 255) {
                    throw new IllegalArgumentException("域名过长: " + host);
                }
                out.writeByte(domainBytes.length);
                out.writeBytes(domainBytes);
            }
            case Socks5Constants.ATYP_IPV6 -> {
                try {
                    out.writeBytes(InetAddress.getByName(host).getAddress());
                } catch (UnknownHostException e) {
                    throw new IllegalArgumentException("无效的 IPv6 地址: " + host, e);
                }
            }
            default -> throw new IllegalArgumentException("不支持的地址类型: " + addressType);
        }
        out.writeShort(port);
    }
}
