package com.xiaoniucode.etp.server.transport.tcp;

import com.xiaoniucode.etp.core.domain.ProxyConfig;
import com.xiaoniucode.etp.core.domain.ProxyConfigExt;
import com.xiaoniucode.etp.server.service.ProxyConfigService;
import com.xiaoniucode.etp.server.statemachine.stream.StreamManager;
import com.xiaoniucode.etp.server.transport.IpCheckHandler;
import com.xiaoniucode.etp.server.security.IpAccessChecker;
import com.xiaoniucode.etp.server.utils.NetUtils;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * IP 访问控制检查
 */
@Component
@ChannelHandler.Sharable
public class TcpIpCheckHandler extends IpCheckHandler {
    private final InternalLogger logger = InternalLoggerFactory.getInstance(TcpIpCheckHandler.class);
    @Autowired
    private ProxyConfigService proxyConfigService;

    @Autowired
    public TcpIpCheckHandler(IpAccessChecker ipAccessChecker, StreamManager streamManager) {
        super(ipAccessChecker, streamManager);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        logger.debug("IP访问控制检查");
        Channel visitor = ctx.channel();
        int remotePort = getListenerPort(visitor);
        String visitorIp = NetUtils.getIp(visitor);
        ProxyConfigExt ext = proxyConfigService.findByListenPort(remotePort);
        if (ext != null) {
            ProxyConfig config = ext.getProxyConfig();
            if (!doCheckAccess(visitor, config)) {
                logger.debug("{} 没有访问权限", visitorIp);
                return;
            }
        }
        ctx.fireChannelActive();
    }
}
