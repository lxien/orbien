package io.github.lxien.orbien.client.health;

import io.netty.channel.Channel;

public final class HealthCheckHolder {
    private static volatile HealthCheckManager INSTANCE;

    private HealthCheckHolder() {
    }

    /**
     * 初始化或更新控制通道。重连时必须传入新的 control，否则健康状态仍会发往已断开的连接。
     */
    public static void init(Channel control) {
        if (control == null) {
            throw new IllegalArgumentException("control cannot be null");
        }

        synchronized (HealthCheckHolder.class) {
            if (INSTANCE == null) {
                INSTANCE = new HealthCheckManager(control);
            } else {
                INSTANCE.updateControlChannel(control);
            }
        }
    }

    public static HealthCheckManager get() {
        HealthCheckManager manager = INSTANCE;
        if (manager == null) {
            throw new IllegalStateException("HealthCheckManager 未初始化！");
        }
        return manager;
    }

    public static void shutdown() {
        synchronized (HealthCheckHolder.class) {
            if (INSTANCE != null) {
                INSTANCE.shutdown();
                INSTANCE = null;
            }
        }
    }
}
