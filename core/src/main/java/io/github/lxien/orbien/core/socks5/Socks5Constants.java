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

/**
 * SOCKS5 协议常量（RFC 1928 / RFC 1929）。
 */
public final class Socks5Constants {

    private Socks5Constants() {
    }

    public static final byte VERSION = 0x05;
    public static final byte AUTH_VERSION = 0x01;

    public static final byte METHOD_NO_AUTH = 0x00;
    public static final byte METHOD_USERNAME_PASSWORD = 0x02;
    public static final byte METHOD_NO_ACCEPTABLE = (byte) 0xFF;

    public static final byte CMD_CONNECT = 0x01;

    public static final byte ATYP_IPV4 = 0x01;
    public static final byte ATYP_DOMAIN = 0x03;
    public static final byte ATYP_IPV6 = 0x04;

    public static final byte REP_SUCCESS = 0x00;
    public static final byte REP_GENERAL_FAILURE = 0x01;
    public static final byte REP_NOT_ALLOWED = 0x02;
    public static final byte REP_NETWORK_UNREACHABLE = 0x03;
    public static final byte REP_HOST_UNREACHABLE = 0x04;
    public static final byte REP_CONNECTION_REFUSED = 0x05;
    public static final byte REP_COMMAND_NOT_SUPPORTED = 0x07;
    public static final byte REP_ADDRESS_TYPE_NOT_SUPPORTED = 0x08;

    public static final byte AUTH_STATUS_SUCCESS = 0x00;
    public static final byte AUTH_STATUS_FAILURE = 0x01;
}
