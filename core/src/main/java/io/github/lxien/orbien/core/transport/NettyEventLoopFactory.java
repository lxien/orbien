package io.github.lxien.orbien.core.transport;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.IoHandlerFactory;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollDatagramChannel;
import io.netty.channel.epoll.EpollIoHandler;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.kqueue.KQueue;
import io.netty.channel.kqueue.KQueueDatagramChannel;
import io.netty.channel.kqueue.KQueueIoHandler;
import io.netty.channel.kqueue.KQueueServerSocketChannel;
import io.netty.channel.kqueue.KQueueSocketChannel;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.channel.uring.IoUring;
import io.netty.channel.uring.IoUringDatagramChannel;
import io.netty.channel.uring.IoUringIoHandler;
import io.netty.channel.uring.IoUringServerSocketChannel;
import io.netty.channel.uring.IoUringSocketChannel;

import java.util.Locale;

/**
 * 按操作系统选择 Netty 原生传输：
 * Linux 优先 io_uring（内核支持时），否则 Epoll；macOS/BSD 用 KQueue；其余用 NIO。
 * <p>
 * Netty 4.2 使用 {@link MultiThreadIoEventLoopGroup} + 传输层 {@link IoHandlerFactory}。
 */
public final class NettyEventLoopFactory {

    private static final NativeTransport NATIVE_TRANSPORT = detectNativeTransport();

    private enum NativeTransport {
        IO_URING, EPOLL, KQUEUE, NIO
    }

    private NettyEventLoopFactory() {
    }

    private static NativeTransport detectNativeTransport() {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (os.contains("linux")) {
            if (isIoUringAvailable()) {
                return NativeTransport.IO_URING;
            }
            if (isEpollAvailable()) {
                return NativeTransport.EPOLL;
            }
            return NativeTransport.NIO;
        }
        if ((os.contains("mac") || os.contains("darwin") || os.contains("bsd")) && isKQueueAvailable()) {
            return NativeTransport.KQUEUE;
        }
        return NativeTransport.NIO;
    }

    private static boolean isIoUringAvailable() {
        try {
            return IoUring.isAvailable();
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean isEpollAvailable() {
        try {
            return Epoll.isAvailable();
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean isKQueueAvailable() {
        try {
            return KQueue.isAvailable();
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static IoHandlerFactory ioHandlerFactory() {
        return switch (NATIVE_TRANSPORT) {
            case IO_URING -> IoUringIoHandler.newFactory();
            case EPOLL -> EpollIoHandler.newFactory();
            case KQUEUE -> KQueueIoHandler.newFactory();
            case NIO -> NioIoHandler.newFactory();
        };
    }

    /**
     * @param threads 0 表示使用 Netty 默认线程数（CPU 核数 * 2）
     */
    public static EventLoopGroup eventLoopGroup(int threads) {
        IoHandlerFactory factory = ioHandlerFactory();
        if (threads <= 0) {
            return new MultiThreadIoEventLoopGroup(factory);
        }
        return new MultiThreadIoEventLoopGroup(threads, factory);
    }

    public static EventLoopGroup eventLoopGroup() {
        return eventLoopGroup(0);
    }

    public static Class<? extends SocketChannel> socketChannelClass() {
        return switch (NATIVE_TRANSPORT) {
            case IO_URING -> IoUringSocketChannel.class;
            case EPOLL -> EpollSocketChannel.class;
            case KQUEUE -> KQueueSocketChannel.class;
            case NIO -> NioSocketChannel.class;
        };
    }

    public static Class<? extends ServerSocketChannel> serverSocketChannelClass() {
        return switch (NATIVE_TRANSPORT) {
            case IO_URING -> IoUringServerSocketChannel.class;
            case EPOLL -> EpollServerSocketChannel.class;
            case KQUEUE -> KQueueServerSocketChannel.class;
            case NIO -> NioServerSocketChannel.class;
        };
    }

    public static Class<? extends DatagramChannel> datagramChannelClass() {
        return switch (NATIVE_TRANSPORT) {
            case IO_URING -> IoUringDatagramChannel.class;
            case EPOLL -> EpollDatagramChannel.class;
            case KQUEUE -> KQueueDatagramChannel.class;
            case NIO -> NioDatagramChannel.class;
        };
    }

    /**
     * 当前选用的原生传输名称，便于日志诊断
     */
    public static String nativeTransportName() {
        return switch (NATIVE_TRANSPORT) {
            case IO_URING -> "io_uring";
            case EPOLL -> "epoll";
            case KQUEUE -> "kqueue";
            case NIO -> "nio";
        };
    }
}
