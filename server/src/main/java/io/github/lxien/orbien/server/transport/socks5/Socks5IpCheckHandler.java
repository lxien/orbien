package io.github.lxien.orbien.server.transport.socks5;

import io.github.lxien.orbien.core.domain.ProxyConfig;
import io.github.lxien.orbien.core.domain.ProxyConfigExt;
import io.github.lxien.orbien.core.enums.ProtocolType;
import io.github.lxien.orbien.server.service.ProxyConfigService;
import io.github.lxien.orbien.server.statemachine.stream.StreamManager;
import io.github.lxien.orbien.server.transport.IpCheckHandler;
import io.github.lxien.orbien.server.security.IpAccessChecker;
import io.github.lxien.orbien.server.utils.NetUtils;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@ChannelHandler.Sharable
public class Socks5IpCheckHandler extends IpCheckHandler {

    private final InternalLogger logger = InternalLoggerFactory.getInstance(Socks5IpCheckHandler.class);

    @Autowired
    private ProxyConfigService proxyConfigService;

    @Autowired
    public Socks5IpCheckHandler(IpAccessChecker ipAccessChecker, StreamManager streamManager) {
        super(ipAccessChecker, streamManager);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        Channel visitor = ctx.channel();
        int remotePort = getListenerPort(visitor);
        String visitorIp = NetUtils.getIp(visitor);
        ProxyConfigExt ext = proxyConfigService.findByListenPort(remotePort, ProtocolType.SOCKS5);
        if (ext != null) {
            ProxyConfig config = ext.getProxyConfig();
            if (!doCheckAccess(visitor, config)) {
                logger.debug("{} 没有 SOCKS5 访问权限", visitorIp);
                return;
            }
        }
        ctx.fireChannelActive();
    }
}
