package io.github.lxien.orbien.server.transport.http;

import io.github.lxien.orbien.common.utils.StringUtils;
import io.github.lxien.orbien.core.domain.ProxyConfigExt;
import io.github.lxien.orbien.core.transport.AttributeKeys;
import io.github.lxien.orbien.server.service.ProxyConfigService;
import io.github.lxien.orbien.server.statemachine.stream.StreamManager;
import io.github.lxien.orbien.server.transport.IpCheckHandler;
import io.github.lxien.orbien.server.security.IpAccessChecker;
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
        logger.debug("IP访问控制检查");
        Channel visitor = ctx.channel();
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
        if (ext != null) {
            if (!doCheckAccess(visitor, ext.getProxyConfig())) {
                logger.debug("{} 没有访问权限", visitorIp);
                ReferenceCountUtil.release(msg);
                return;
            }
        }
        ctx.fireChannelRead(msg);
    }
}
