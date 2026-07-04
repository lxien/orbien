package io.github.lxien.orbien.server.transport.udp;

import io.github.lxien.orbien.core.domain.ProxyConfig;
import io.github.lxien.orbien.core.domain.ProxyConfigExt;
import io.github.lxien.orbien.core.enums.ProtocolType;
import io.github.lxien.orbien.core.transport.UdpSessionKey;
import io.github.lxien.orbien.server.service.ProxyConfigService;
import io.github.lxien.orbien.server.statemachine.stream.StreamManager;
import io.github.lxien.orbien.server.transport.IpCheckHandler;
import io.github.lxien.orbien.server.security.IpAccessChecker;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;

/**
 * UDP IP 访问控制检查
 */
@Component
@ChannelHandler.Sharable
public class UdpIpCheckHandler extends IpCheckHandler {
    private final InternalLogger logger = InternalLoggerFactory.getInstance(UdpIpCheckHandler.class);
    @Autowired
    private ProxyConfigService proxyConfigService;
    @Autowired
    private IpAccessChecker ipAccessChecker;
    @Autowired
    private StreamManager streamManager;

    @Autowired
    public UdpIpCheckHandler(IpAccessChecker ipAccessChecker, StreamManager streamManager) {
        super(ipAccessChecker, streamManager);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (!(msg instanceof DatagramPacket packet)) {
            ctx.fireChannelRead(msg);
            return;
        }
        Channel channel = ctx.channel();
        int listenPort = getListenerPort(channel);
        InetSocketAddress sender = packet.sender();
        UdpSessionKey sessionKey = UdpSessionKey.of(listenPort, sender);
        if (streamManager.getStreamContextByUdpSession(sessionKey).isPresent()) {
            ctx.fireChannelRead(msg);
            return;
        }

        ProxyConfigExt ext = proxyConfigService.findByListenPort(listenPort, ProtocolType.UDP);
        if (ext != null) {
            ProxyConfig config = ext.getProxyConfig();
            String visitorIp = sender.getAddress().getHostAddress();
            if (!ipAccessChecker.checkAccess(config.getProxyId(), config.getAccessControl(), visitorIp)) {
                logger.debug("[UDP] {} 没有访问权限", visitorIp);
                packet.release();
                return;
            }
        }
        ctx.fireChannelRead(msg);
    }
}
