package io.github.lxien.orbien.core.server;

/**
 * Netty 容器服务生命周期，启动、停止
 *
 * @author lxien
 */
public interface Lifecycle {
    /**
     * 启动容器服务
     */
    void start();

    /**
     * 停止服务 清理资源
     */
    void stop();
}
