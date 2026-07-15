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
package io.github.lxien.orbien.server.transport.http;

import io.github.lxien.orbien.core.domain.ProxyConfigExt;
import io.github.lxien.orbien.core.transport.AttributeKeys;
import io.github.lxien.orbien.core.utils.StringUtils;
import io.github.lxien.orbien.server.security.IpAccessChecker;
import io.github.lxien.orbien.server.service.ProxyConfigService;
import io.github.lxien.orbien.server.statemachine.stream.StreamManager;
import io.github.lxien.orbien.server.transport.IpCheckHandler;
import io.github.lxien.orbien.server.utils.NetUtils;
import io.github.lxien.orbien.server.vhost.DomainRegistry;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@ChannelHandler.Sharable
public class HttpIpCheckHandler extends IpCheckHandler {
    private final InternalLogger logger = InternalLoggerFactory.getInstance(HttpIpCheckHandler.class);
    @Autowired
    private ProxyConfigService proxyConfigService;
    @Autowired
    private DomainRegistry domainRegistry;

    @Autowired
    public HttpIpCheckHandler(IpAccessChecker ipAccessChecker, StreamManager streamManager) {
        super(ipAccessChecker, streamManager);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        Channel visitor = ctx.channel();
        if (Boolean.TRUE.equals(visitor.attr(AttributeKeys.IP_ACCESS_PASSED).get())) {
            ctx.fireChannelRead(msg);
            return;
        }

        String visitorIp = NetUtils.getIp(visitor);
        String domain = visitor.attr(AttributeKeys.VISIT_DOMAIN).get();
        String proxyId = domainRegistry.getProxyIdByDomain(domain);
        if (!StringUtils.hasText(proxyId)) {
            logger.debug("{} 访问域名 {} 没有对应的代理配置", visitorIp, domain);
            ReferenceCountUtil.release(msg);
            ctx.close();
            return;
        }
        ProxyConfigExt ext = proxyConfigService.findById(proxyId);
        if (ext != null && !doCheckAccess(visitor, ext.getProxyConfig())) {
            logger.debug("{} 没有访问权限", visitorIp);
            ReferenceCountUtil.release(msg);
            return;
        }

        visitor.attr(AttributeKeys.IP_ACCESS_PASSED).set(Boolean.TRUE);
        if (ctx.pipeline().context(this) != null) {
            ctx.pipeline().remove(this);
        }
        ctx.fireChannelRead(msg);
    }
}
