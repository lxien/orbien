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

import com.alibaba.cola.statemachine.StateMachine;
import io.github.lxien.orbien.core.codec.NewStreamCodec;
import io.github.lxien.orbien.core.domain.ProxyConfig;
import io.github.lxien.orbien.core.domain.ProxyConfigExt;
import io.github.lxien.orbien.core.domain.Target;
import io.github.lxien.orbien.core.enums.ProtocolType;
import io.github.lxien.orbien.core.socks5.Socks5Address;
import io.github.lxien.orbien.core.socks5.Socks5Constants;
import io.github.lxien.orbien.core.transport.AttributeKeys;
import io.github.lxien.orbien.core.utils.ChannelUtils;
import io.github.lxien.orbien.server.service.ProxyConfigService;
import io.github.lxien.orbien.server.statemachine.stream.StreamConstants;
import io.github.lxien.orbien.server.statemachine.stream.StreamContext;
import io.github.lxien.orbien.server.statemachine.stream.StreamEvent;
import io.github.lxien.orbien.server.statemachine.stream.StreamManager;
import io.github.lxien.orbien.server.statemachine.stream.StreamState;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.AttributeKey;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

/**
 * SOCKS5 握手：方法协商 -> 认证 -> CONNECT。
 */
@Component
@ChannelHandler.Sharable
public class Socks5HandshakeHandler extends SimpleChannelInboundHandler<ByteBuf> {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(Socks5HandshakeHandler.class);
    private static final AttributeKey<HandshakeState> STATE = AttributeKey.valueOf("orbien.socks5.handshake.state");
    private static final AttributeKey<ProxyConfig> PROXY_CONFIG = AttributeKey.valueOf("orbien.socks5.proxy.config");

    @Autowired
    private StreamManager streamManager;
    @Autowired
    private ProxyConfigService proxyConfigService;
    @Autowired
    private Socks5AuthValidator socks5AuthValidator;
    @Autowired
    @Qualifier("streamStateMachine")
    private StateMachine<StreamState, StreamEvent, StreamContext> stateMachine;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf in) {
        Channel visitor = ctx.channel();
        HandshakeState state = visitor.attr(STATE).get();
        if (state == null) {
            state = HandshakeState.INIT;
            visitor.attr(STATE).set(state);
            loadProxyConfig(visitor);
        }

        try {
            switch (state) {
                case INIT -> handleInit(ctx, in);
                case AUTH -> handleAuth(ctx, in);
                case COMMAND -> handleCommand(ctx, in);
                default -> ChannelUtils.closeOnFlush(visitor);
            }
        } catch (Exception ex) {
            logger.debug("[SOCKS5] 握手失败: {}", ex.getMessage());
            ChannelUtils.closeOnFlush(visitor);
        }
    }

    private void loadProxyConfig(Channel visitor) {
        int listenPort = getListenerPort(visitor);
        ProxyConfigExt ext = proxyConfigService.findByListenPort(listenPort, ProtocolType.SOCKS5);
        if (ext != null) {
            visitor.attr(PROXY_CONFIG).set(ext.getProxyConfig());
        }
    }

    private void handleInit(ChannelHandlerContext ctx, ByteBuf in) {
        if (in.readableBytes() < 2) {
            return;
        }
        in.markReaderIndex();
        byte version = in.readByte();
        if (version != Socks5Constants.VERSION) {
            ChannelUtils.closeOnFlush(ctx.channel());
            return;
        }
        int methodCount = in.readUnsignedByte();
        if (in.readableBytes() < methodCount) {
            in.resetReaderIndex();
            return;
        }
        byte[] methods = new byte[methodCount];
        in.readBytes(methods);

        ProxyConfig config = ctx.channel().attr(PROXY_CONFIG).get();
        byte selected = socks5AuthValidator.selectAuthMethod(config);
        boolean supported = false;
        for (byte method : methods) {
            if (method == selected) {
                supported = true;
                break;
            }
        }
        if (!supported && selected == Socks5Constants.METHOD_NO_AUTH) {
            for (byte method : methods) {
                if (method == Socks5Constants.METHOD_NO_AUTH) {
                    supported = true;
                    selected = Socks5Constants.METHOD_NO_AUTH;
                    break;
                }
            }
        }
        if (!supported) {
            Socks5ReplyHelper.sendMethodReject(ctx.channel());
            return;
        }

        ByteBuf reply = ctx.alloc().buffer(2);
        reply.writeByte(Socks5Constants.VERSION);
        reply.writeByte(selected);
        ctx.writeAndFlush(reply);

        if (selected == Socks5Constants.METHOD_USERNAME_PASSWORD) {
            ctx.channel().attr(STATE).set(HandshakeState.AUTH);
        } else {
            ctx.channel().attr(STATE).set(HandshakeState.COMMAND);
        }
    }

    private void handleAuth(ChannelHandlerContext ctx, ByteBuf in) {
        if (in.readableBytes() < 2) {
            return;
        }
        in.markReaderIndex();
        byte version = in.readByte();
        if (version != Socks5Constants.AUTH_VERSION) {
            Socks5ReplyHelper.sendAuthFailure(ctx.channel());
            return;
        }
        int ulen = in.readUnsignedByte();
        if (in.readableBytes() < ulen + 1) {
            in.resetReaderIndex();
            return;
        }
        String username = in.readCharSequence(ulen, StandardCharsets.US_ASCII).toString();
        int plen = in.readUnsignedByte();
        if (in.readableBytes() < plen) {
            in.resetReaderIndex();
            return;
        }
        String password = in.readCharSequence(plen, StandardCharsets.US_ASCII).toString();

        ProxyConfig config = ctx.channel().attr(PROXY_CONFIG).get();
        if (!socks5AuthValidator.authenticate(config, username, password)) {
            Socks5ReplyHelper.sendAuthFailure(ctx.channel());
            return;
        }

        ByteBuf reply = ctx.alloc().buffer(2);
        reply.writeByte(Socks5Constants.AUTH_VERSION);
        reply.writeByte(Socks5Constants.AUTH_STATUS_SUCCESS);
        ctx.writeAndFlush(reply);
        ctx.channel().attr(STATE).set(HandshakeState.COMMAND);
    }

    private void handleCommand(ChannelHandlerContext ctx, ByteBuf in) {
        if (in.readableBytes() < 4) {
            return;
        }
        in.markReaderIndex();
        byte version = in.readByte();
        byte command = in.readByte();
        in.readByte(); // RSV
        if (version != Socks5Constants.VERSION) {
            Socks5ReplyHelper.failAndClose(ctx, Socks5Constants.REP_GENERAL_FAILURE);
            return;
        }
        if (command != Socks5Constants.CMD_CONNECT) {
            Socks5ReplyHelper.failAndClose(ctx, Socks5Constants.REP_COMMAND_NOT_SUPPORTED);
            return;
        }
        if (in.readableBytes() < 1) {
            in.resetReaderIndex();
            return;
        }

        Socks5Address address;
        try {
            address = Socks5Address.decode(in);
        } catch (Exception ex) {
            Socks5ReplyHelper.failAndClose(ctx, Socks5Constants.REP_ADDRESS_TYPE_NOT_SUPPORTED);
            return;
        }

        Channel visitor = ctx.channel();
        visitor.attr(STATE).set(HandshakeState.DONE);
        visitor.attr(AttributeKeys.PROTOCOL_TYPE).set(ProtocolType.SOCKS5);

        String targetHost = normalizeSocks5Host(address.host());
        StreamContext streamContext = streamManager.createStreamContext(visitor, stateMachine);
        streamContext.setProtocol(ProtocolType.SOCKS5);
        streamContext.setStreamManager(streamManager);
        streamContext.setTarget(new Target(targetHost, address.port()));
        streamContext.setVariable(StreamConstants.SOCKS5_AWAIT_REPLY, Boolean.TRUE);

        logger.debug("[SOCKS5] CONNECT {}:{} streamId={}", targetHost, address.port(), streamContext.getStreamId());
        streamContext.fireEvent(StreamEvent.STREAM_OPEN);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        logger.debug("[SOCKS5] 握手异常", cause);
        ChannelUtils.closeOnFlush(ctx.channel());
    }

    private int getListenerPort(Channel visitor) {
        InetSocketAddress sa = (InetSocketAddress) visitor.localAddress();
        return sa.getPort();
    }

    /**
     * localhost / IPv6 loopback 归一为 127.0.0.1
     */
    static String normalizeSocks5Host(String host) {
        return NewStreamCodec.normalizeLocalIp(host);
    }

    private enum HandshakeState {
        INIT, AUTH, COMMAND, DONE
    }
}
