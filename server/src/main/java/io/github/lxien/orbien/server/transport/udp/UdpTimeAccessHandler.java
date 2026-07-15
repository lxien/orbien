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
package io.github.lxien.orbien.server.transport.udp;

import io.github.lxien.orbien.core.domain.ProxyConfig;
import io.github.lxien.orbien.core.domain.ProxyConfigExt;
import io.github.lxien.orbien.core.enums.ProtocolType;
import io.github.lxien.orbien.core.transport.UdpSessionKey;
import io.github.lxien.orbien.server.security.TimeAccessChecker;
import io.github.lxien.orbien.server.service.ProxyConfigService;
import io.github.lxien.orbien.server.statemachine.stream.StreamEvent;
import io.github.lxien.orbien.server.statemachine.stream.StreamManager;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.socket.DatagramPacket;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;

@Component
@ChannelHandler.Sharable
public class UdpTimeAccessHandler extends ChannelInboundHandlerAdapter {
    private final InternalLogger logger = InternalLoggerFactory.getInstance(UdpTimeAccessHandler.class);

    @Autowired
    private ProxyConfigService proxyConfigService;
    @Autowired
    private TimeAccessChecker timeAccessChecker;
    @Autowired
    private StreamManager streamManager;

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (!(msg instanceof DatagramPacket packet)) {
            ctx.fireChannelRead(msg);
            return;
        }
        Channel channel = ctx.channel();
        InetSocketAddress local = (InetSocketAddress) channel.localAddress();
        int listenPort = local.getPort();
        InetSocketAddress sender = packet.sender();
        UdpSessionKey sessionKey = UdpSessionKey.of(listenPort, sender);
        // 已有会话仍需检查：时间窗可能已关闭
        ProxyConfigExt ext = proxyConfigService.findByListenPort(listenPort, ProtocolType.UDP);
        if (ext != null) {
            ProxyConfig config = ext.getProxyConfig();
            if (!timeAccessChecker.checkAccess(config.getProxyId(), config.getTimeAccess())) {
                logger.debug("[UDP] {} 触发时间访问限制", sender.getAddress().getHostAddress());
                packet.release();
                streamManager.getStreamContextByUdpSession(sessionKey).ifPresent(context ->
                        context.fireEvent(StreamEvent.STREAM_LOCAL_CLOSE));
                return;
            }
        }
        ctx.fireChannelRead(msg);
    }
}
