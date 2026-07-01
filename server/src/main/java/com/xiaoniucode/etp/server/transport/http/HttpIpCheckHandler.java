package com.xiaoniucode.etp.server.transport.http;

import com.xiaoniucode.etp.core.domain.ProxyConfig;
import com.xiaoniucode.etp.core.domain.ProxyConfigExt;
import com.xiaoniucode.etp.core.transport.AttributeKeys;
import com.xiaoniucode.etp.server.service.ProxyConfigService;
import com.xiaoniucode.etp.server.statemachine.stream.StreamManager;
import com.xiaoniucode.etp.server.transport.IpCheckHandler;
import com.xiaoniucode.etp.server.security.IpAccessChecker;
import com.xiaoniucode.etp.server.utils.NetUtils;
import com.xiaoniucode.etp.server.vhost.DomainRegistry;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

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
        ProxyConfigExt ext = proxyConfigService.findById(proxyId);
        if (ext != null) {
            if (!doCheckAccess(visitor, ext.getProxyConfig())) {
                logger.debug("{} 没有访问权限", visitorIp);
                return;
            }
        }
        ctx.fireChannelRead(msg);
    }
}
