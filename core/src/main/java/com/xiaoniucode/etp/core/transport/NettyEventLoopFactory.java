package com.xiaoniucode.etp.core.transport;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.*;
import io.netty.channel.kqueue.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.*;

/**
 * 根据不同的操作系统创建 EventLoop，Linux采用Epoll、MacOS采用KQueue、Windows/其他采用NIO
 *
 * @author liuxin
 */
public final class NettyEventLoopFactory {
    private NettyEventLoopFactory() {
    }

    public static EventLoopGroup eventLoopGroup(Integer threads) {
        if (Epoll.isAvailable()) {
            return new EpollEventLoopGroup(threads);
        } else if (KQueue.isAvailable()) {
            return new KQueueEventLoopGroup(threads);
        } else {
            return new NioEventLoopGroup(threads);
        }
    }

    public static EventLoopGroup eventLoopGroup() {
        //设置为0 netty会采用默认的线程配置
        return eventLoopGroup(0);
    }

    public static Class<? extends SocketChannel> socketChannelClass() {
        if (Epoll.isAvailable()) {
            return EpollSocketChannel.class;
        } else if (KQueue.isAvailable()) {
            return KQueueSocketChannel.class;
        } else {
            return NioSocketChannel.class;
        }
    }

    public static Class<? extends ServerSocketChannel> serverSocketChannelClass() {
        if (Epoll.isAvailable()) {
            return EpollServerSocketChannel.class;
        } else if (KQueue.isAvailable()) {
            return KQueueServerSocketChannel.class;
        } else {
            return NioServerSocketChannel.class;
        }
    }

    public static Class<? extends DatagramChannel> datagramChannelClass() {
        if (Epoll.isAvailable()) {
            return EpollDatagramChannel.class;
        } else if (KQueue.isAvailable()) {
            return KQueueDatagramChannel.class;
        } else {
            return NioDatagramChannel.class;
        }
    }
}
