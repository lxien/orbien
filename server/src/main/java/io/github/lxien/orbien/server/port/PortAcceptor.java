package io.github.lxien.orbien.server.port;

import io.github.lxien.orbien.server.configuration.SpringContextHolder;
import io.github.lxien.orbien.server.transport.socks5.Socks5ProxyServer;
import io.github.lxien.orbien.server.transport.tcp.TcpProxyServer;
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
    private final Map<Integer, PortBindingType> portBindingTypes = new ConcurrentHashMap<>();

    public void bindPort(final Integer listenPort) {
        bindTcpPort(listenPort);
    }

    public void bindTcpPort(final Integer listenPort) {
        bindInternal(listenPort, PortBindingType.TCP);
    }

    public void bindSocks5Port(final Integer listenPort) {
        bindInternal(listenPort, PortBindingType.SOCKS5);
    }

    private void bindInternal(final Integer listenPort, PortBindingType bindingType) {
        if (listenPort < 0 || listenPort > 65535) {
            logger.warn("尝试绑定非法端口: {}", listenPort);
            return;
        }
        PortBindingType existingType = portBindingTypes.get(listenPort);
        Channel existingChannel = portToChannel.get(listenPort);
        if (existingChannel != null && existingChannel.isActive()) {
            if (existingType == bindingType) {
                logger.debug("端口 {} 已按 {} 协议绑定，跳过重复绑定", listenPort, bindingType);
                return;
            }
            logger.warn("端口 {} 已按 {} 绑定，将重新绑定为 {}", listenPort, existingType, bindingType);
            closeChannel(listenPort, existingChannel);
        }
        try {
            ServerBootstrap serverBootstrap = switch (bindingType) {
                case SOCKS5 -> SpringContextHolder.getBean(Socks5ProxyServer.class).getServerBootstrap();
                case TCP -> SpringContextHolder.getBean(TcpProxyServer.class).getServerBootstrap();
            };
            ChannelFuture future = serverBootstrap.bind(listenPort).sync();
            Channel channel = future.channel();
            portToChannel.put(listenPort, channel);
            portBindingTypes.put(listenPort, bindingType);
            logger.debug("端口 {} 绑定成功，协议={}", listenPort, bindingType);
        } catch (Throwable t) {
            logger.error("绑定端口 {} 失败", listenPort, t);
        }
    }

    public void stopPortListen(final Integer listenPort) {
        if (listenPort < 0 || listenPort > 65535) {
            logger.warn("尝试停止非法端口: {}", listenPort);
            return;
        }
        Channel channel = portToChannel.remove(listenPort);
        portBindingTypes.remove(listenPort);
        if (channel != null) {
            closeChannel(listenPort, channel);
            logger.debug("停止端口监听成功: {}", listenPort);
        } else {
            logger.debug("要停止的端口{}未被绑定，无需操作。", listenPort);
        }
    }

    private void closeChannel(Integer listenPort, Channel channel) {
        try {
            channel.close().sync();
        } catch (Throwable t) {
            logger.error("停止服务失败：端口-{}", listenPort, t);
        }
    }

    @PreDestroy
    public void clearAll() {
        logger.debug("开始清理所有代理端口资源占用...");
        for (Map.Entry<Integer, Channel> entry : portToChannel.entrySet()) {
            Integer port = entry.getKey();
            Channel channel = entry.getValue();
            closeChannel(port, channel);
            logger.info("成功释放端口: {}", port);
        }
        portToChannel.clear();
        portBindingTypes.clear();
        logger.debug("代理端口资源清理完毕。");
    }

    private enum PortBindingType {
        TCP, SOCKS5
    }
}