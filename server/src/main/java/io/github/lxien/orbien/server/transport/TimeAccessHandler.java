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
package io.github.lxien.orbien.server.transport;

import io.github.lxien.orbien.core.domain.ProxyConfig;
import io.github.lxien.orbien.core.domain.TimeAccessConfig;
import io.github.lxien.orbien.core.enums.ProtocolType;
import io.github.lxien.orbien.core.transport.AttributeKeys;
import io.github.lxien.orbien.core.utils.ChannelUtils;
import io.github.lxien.orbien.server.security.TimeAccessChecker;
import io.github.lxien.orbien.server.statemachine.stream.StreamEvent;
import io.github.lxien.orbien.server.statemachine.stream.StreamManager;
import io.github.lxien.orbien.server.utils.NetUtils;
import io.github.lxien.orbien.server.utils.NettyHttpUtils;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.net.InetSocketAddress;

public abstract class TimeAccessHandler extends ChannelInboundHandlerAdapter {
    private final InternalLogger logger = InternalLoggerFactory.getInstance(getClass());
    protected final TimeAccessChecker timeAccessChecker;
    protected final StreamManager streamManager;

    protected TimeAccessHandler(TimeAccessChecker timeAccessChecker, StreamManager streamManager) {
        this.timeAccessChecker = timeAccessChecker;
        this.streamManager = streamManager;
    }

    protected int getListenerPort(Channel visitor) {
        InetSocketAddress sa = (InetSocketAddress) visitor.localAddress();
        return sa.getPort();
    }

    protected boolean shouldRecheck(Channel visitor) {
        Long seenGen = visitor.attr(AttributeKeys.TIME_ACCESS_POLICY_GEN).get();
        if (seenGen == null || seenGen != timeAccessChecker.generation()) {
            return true;
        }
        Long nextAt = visitor.attr(AttributeKeys.TIME_ACCESS_NEXT_CHECK_AT).get();
        return nextAt == null || System.currentTimeMillis() >= nextAt;
    }

    protected boolean doCheckAccess(Channel visitor, ProxyConfig config) {
        if (config == null) {
            return true;
        }
        TimeAccessConfig timeAccess = config.getTimeAccess();
        boolean allowed = timeAccessChecker.checkAccess(config.getProxyId(), timeAccess);
        if (!allowed) {
            String visitorIp = NetUtils.getIp(visitor);
            logger.debug("来源 {} 触发时间访问限制, proxy={}", visitorIp, config.getName());
            deny(visitor, config);
            return false;
        }
        visitor.attr(AttributeKeys.TIME_ACCESS_POLICY_GEN).set(timeAccessChecker.generation());
        visitor.attr(AttributeKeys.TIME_ACCESS_NEXT_CHECK_AT)
                .set(timeAccessChecker.nextCheckAtMillis(config.getProxyId(), timeAccess));
        return true;
    }

    private void deny(Channel visitor, ProxyConfig config) {
        ProtocolType protocol = config.getProtocol();
        if (protocol != null && (protocol.isHttp() || protocol.isHttps() || protocol.isFile())) {
            NettyHttpUtils.sendHttp403(visitor).addListener(future -> ChannelUtils.closeOnFlush(visitor));
        } else {
            ChannelUtils.closeOnFlush(visitor);
        }
        streamManager.getStreamContext(visitor).ifPresent(context ->
                context.fireEvent(StreamEvent.STREAM_LOCAL_CLOSE));
    }
}
