package com.xiaoniucode.etp.server.port;

import com.xiaoniucode.etp.server.configuration.SpringContextHolder;
import com.xiaoniucode.etp.server.transport.tcp.TcpProxyServer;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import jakarta.annotation.PreDestroy;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class PortAcceptor {
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(PortAcceptor.class);
    private final Map<Integer, Channel> portToChannel = new ConcurrentHashMap<>();

    public void bindPort(final Integer listenPort) {
        if (listenPort < 0 || listenPort > 65535) {
            logger.warn("尝试绑定非法端口: {}", listenPort);
            return;
        }
        portToChannel.computeIfAbsent(listenPort, key -> {
            try {
                TcpProxyServer tcpProxyServer = SpringContextHolder.getBean(TcpProxyServer.class);
                ServerBootstrap serverBootstrap = tcpProxyServer.getServerBootstrap();
                ChannelFuture future = serverBootstrap.bind(listenPort).sync();
                return future.channel();
            } catch (Throwable t) {
                logger.error("绑定端口 {} 失败", listenPort, t);
                return null;
            }
        });
    }

    public void stopPortListen( final Integer listenPort) {
        if (listenPort < 0 || listenPort > 65535) {
            logger.warn("尝试停止非法端口: {}", listenPort);
            return;
        }
        Channel channel = portToChannel.remove(listenPort);
        if (channel != null) {
            try {
                channel.close().sync();
                logger.debug("停止端口监听成功: {}", listenPort);
            } catch (Throwable t) {
                logger.error("停止服务失败：端口-{}", listenPort, t);
            }
        } else {
            logger.debug("要停止的端口{}未被绑定，无需操作。", listenPort);
        }
    }

    @PreDestroy
    public void clearAll() {
        logger.debug("开始清理所有代理端口资源占用...");
        for (Map.Entry<Integer, Channel> entry : portToChannel.entrySet()) {
            Integer port = entry.getKey();
            Channel channel = entry.getValue();
            try {
                channel.close().sync();
                logger.info("成功释放端口: {}", port);
            } catch (Throwable t) {
                logger.error("关闭端口 {} 的 channel 失败", port, t);
            }
        }
        portToChannel.clear();
        logger.debug("代理端口资源清理完毕。");
    }
}