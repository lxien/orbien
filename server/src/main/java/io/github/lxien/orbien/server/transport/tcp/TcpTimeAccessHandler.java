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
package io.github.lxien.orbien.server.transport.tcp;

import io.github.lxien.orbien.core.domain.ProxyConfigExt;
import io.github.lxien.orbien.core.enums.ProtocolType;
import io.github.lxien.orbien.server.security.TimeAccessChecker;
import io.github.lxien.orbien.server.service.ProxyConfigService;
import io.github.lxien.orbien.server.statemachine.stream.StreamManager;
import io.github.lxien.orbien.server.transport.TimeAccessHandler;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@ChannelHandler.Sharable
public class TcpTimeAccessHandler extends TimeAccessHandler {
    @Autowired
    private ProxyConfigService proxyConfigService;

    @Autowired
    public TcpTimeAccessHandler(TimeAccessChecker timeAccessChecker, StreamManager streamManager) {
        super(timeAccessChecker, streamManager);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        Channel visitor = ctx.channel();
        ProxyConfigExt ext = proxyConfigService.findByListenPort(getListenerPort(visitor), ProtocolType.TCP);
        if (ext != null && !doCheckAccess(visitor, ext.getProxyConfig())) {
            return;
        }
        ctx.fireChannelActive();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        Channel visitor = ctx.channel();
        if (!shouldRecheck(visitor)) {
            ctx.fireChannelRead(msg);
            return;
        }
        ProxyConfigExt ext = proxyConfigService.findByListenPort(getListenerPort(visitor), ProtocolType.TCP);
        if (ext != null && !doCheckAccess(visitor, ext.getProxyConfig())) {
            io.netty.util.ReferenceCountUtil.release(msg);
            return;
        }
        ctx.fireChannelRead(msg);
    }
}
