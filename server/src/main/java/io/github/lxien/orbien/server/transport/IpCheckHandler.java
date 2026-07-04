package io.github.lxien.orbien.server.transport;

import io.github.lxien.orbien.core.domain.ProxyConfig;
import io.github.lxien.orbien.core.enums.ProtocolType;
import io.github.lxien.orbien.core.utils.ChannelUtils;
import io.github.lxien.orbien.server.security.IpAccessChecker;
import io.github.lxien.orbien.server.statemachine.stream.StreamEvent;
import io.github.lxien.orbien.server.statemachine.stream.StreamManager;
import io.github.lxien.orbien.server.utils.NetUtils;
import io.github.lxien.orbien.server.utils.NettyHttpUtils;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.net.InetSocketAddress;

/**
 * IP 访问控制检查
 */
public abstract class IpCheckHandler extends ChannelInboundHandlerAdapter {
    private final InternalLogger logger = InternalLoggerFactory.getInstance(IpCheckHandler.class);
    private final IpAccessChecker ipAccessChecker;
    private final StreamManager streamManager;

    public IpCheckHandler(IpAccessChecker ipAccessChecker, StreamManager streamManager) {
        this.ipAccessChecker = ipAccessChecker;
        this.streamManager = streamManager;
    }

    /**
     * 获取服务器监听端口
     *
     * @param visitor 管道
     * @return 监听端口
     */
    protected int getListenerPort(Channel visitor) {
        InetSocketAddress sa = (InetSocketAddress) visitor.localAddress();
        return sa.getPort();
    }

    protected boolean doCheckAccess(Channel visitor, ProxyConfig config) {
        String visitorIp = NetUtils.getIp(visitor);
        boolean checkAccess = ipAccessChecker.checkAccess(config.getProxyId(),config.getAccessControl(), visitorIp);
        if (!checkAccess) {
            logger.debug("来源IP {} 无访问权限", visitorIp);
            ProtocolType protocol = config.getProtocol();
            if (protocol.isHttp()) {
                NettyHttpUtils.sendHttp403(visitor).addListener(future -> {
                    ChannelUtils.closeOnFlush(visitor);
                });
            } else if (protocol.isTcp()) {
                ChannelUtils.closeOnFlush(visitor);
            }
            //尝试关闭流，可能之前已经建立过连接，后来权限发生变化
            streamManager.getStreamContext(visitor).ifPresent(context -> {
                logger.debug("没有隧道访问权限，关闭 {} 流", config.getName());
                context.fireEvent(StreamEvent.STREAM_LOCAL_CLOSE);
            });
            return false;
        }
        return true;
    }
}
