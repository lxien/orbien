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

package io.github.lxien.orbien.server.transport.socks5;

import io.github.lxien.orbien.core.socks5.Socks5Address;
import io.github.lxien.orbien.core.socks5.Socks5Constants;
import io.github.lxien.orbien.core.transport.NettyConstants;
import io.github.lxien.orbien.core.utils.ChannelUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.net.InetSocketAddress;

/**
 * SOCKS5 CONNECT 响应与 pipeline 切换辅助类。
 */
public final class Socks5ReplyHelper {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(Socks5ReplyHelper.class);

    private Socks5ReplyHelper() {
    }

    public static void sendConnectSuccess(Channel visitor) {
        if (visitor == null || !visitor.isActive()) {
            return;
        }
        ByteBuf reply = Unpooled.buffer(10);
        reply.writeByte(Socks5Constants.VERSION);
        reply.writeByte(Socks5Constants.REP_SUCCESS);
        reply.writeByte(0x00);
        reply.writeByte(Socks5Constants.ATYP_IPV4);
        reply.writeInt(0);
        reply.writeShort(0);
        visitor.writeAndFlush(reply);
        switchToRelay(visitor);
        logger.debug("[SOCKS5] CONNECT 成功，已发送响应");
    }

    public static void sendConnectFailure(Channel visitor, byte replyCode) {
        if (visitor == null || !visitor.isActive()) {
            return;
        }
        ByteBuf reply = Unpooled.buffer(10);
        reply.writeByte(Socks5Constants.VERSION);
        reply.writeByte(replyCode);
        reply.writeByte(0x00);
        reply.writeByte(Socks5Constants.ATYP_IPV4);
        reply.writeInt(0);
        reply.writeShort(0);
        visitor.writeAndFlush(reply).addListener(f -> ChannelUtils.closeOnFlush(visitor));
    }

    public static void sendAuthFailure(Channel visitor) {
        if (visitor == null || !visitor.isActive()) {
            return;
        }
        ByteBuf reply = Unpooled.buffer(2);
        reply.writeByte(Socks5Constants.AUTH_VERSION);
        reply.writeByte(Socks5Constants.AUTH_STATUS_FAILURE);
        visitor.writeAndFlush(reply).addListener(f -> ChannelUtils.closeOnFlush(visitor));
    }

    public static void sendMethodReject(Channel visitor) {
        if (visitor == null || !visitor.isActive()) {
            return;
        }
        ByteBuf reply = Unpooled.buffer(2);
        reply.writeByte(Socks5Constants.VERSION);
        reply.writeByte(Socks5Constants.METHOD_NO_ACCEPTABLE);
        visitor.writeAndFlush(reply).addListener(f -> ChannelUtils.closeOnFlush(visitor));
    }

    public static void switchToRelay(Channel visitor) {
        ChannelPipeline pipeline = visitor.pipeline();
        if (pipeline.get(NettyConstants.SOCKS5_HANDSHAKE_HANDLER) != null) {
            pipeline.remove(NettyConstants.SOCKS5_HANDSHAKE_HANDLER);
        }
    }

    public static Socks5Address boundAddress(Channel visitor) {
        if (visitor.localAddress() instanceof InetSocketAddress local) {
            return new Socks5Address(Socks5Constants.ATYP_IPV4, local.getAddress().getHostAddress(), local.getPort());
        }
        return new Socks5Address(Socks5Constants.ATYP_IPV4, "0.0.0.0", 0);
    }

    public static void failAndClose(ChannelHandlerContext ctx, byte replyCode) {
        sendConnectFailure(ctx.channel(), replyCode);
    }
}
