package com.xiaoniucode.etp.server.port;

import com.xiaoniucode.etp.server.configuration.SpringContextHolder;
import com.xiaoniucode.etp.server.transport.udp.UdpProxyServer;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import jakarta.annotation.PreDestroy;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class UdpPortAcceptor {
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(UdpPortAcceptor.class);
    private final Map<Integer, Channel> portToChannel = new ConcurrentHashMap<>();

    public void bindPort(final Integer listenPort) {
        if (listenPort < 0 || listenPort > 65535) {
            logger.warn("尝试绑定非法 UDP 端口: {}", listenPort);
            return;
        }
        portToChannel.computeIfAbsent(listenPort, key -> {
            try {
                UdpProxyServer udpProxyServer = SpringContextHolder.getBean(UdpProxyServer.class);
                Bootstrap serverBootstrap = udpProxyServer.getServerBootstrap();
                ChannelFuture future = serverBootstrap.bind(new InetSocketAddress(listenPort)).sync();
                logger.debug("UDP 端口 {} 绑定成功", listenPort);
                return future.channel();
            } catch (Throwable t) {
                logger.error("绑定 UDP 端口 {} 失败", listenPort, t);
                return null;
            }
        });
    }

    public void stopPortListen(final Integer listenPort) {
        if (listenPort < 0 || listenPort > 65535) {
            logger.warn("尝试停止非法 UDP 端口: {}", listenPort);
            return;
        }
        Channel channel = portToChannel.remove(listenPort);
        if (channel != null) {
            try {
                channel.close().sync();
                logger.debug("停止 UDP 端口监听成功: {}", listenPort);
            } catch (Throwable t) {
                logger.error("停止 UDP 服务失败：端口-{}", listenPort, t);
            }
        }
    }

    @PreDestroy
    public void clearAll() {
        for (Map.Entry<Integer, Channel> entry : portToChannel.entrySet()) {
            try {
                entry.getValue().close().sync();
            } catch (Throwable t) {
                logger.error("关闭 UDP 端口 {} 失败", entry.getKey(), t);
            }
        }
        portToChannel.clear();
    }
}
