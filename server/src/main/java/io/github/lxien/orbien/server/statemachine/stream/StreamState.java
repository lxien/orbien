package io.github.lxien.orbien.server.statemachine.stream;

public enum StreamState {
    /**
     * 初始状态
     */
    IDLE,

    /**
     * 打开中
     */
    OPENING,

    /**
     * 已打开
     */
    OPENED,
    /**
     * 暂停
     */
    PAUSED,
    /**
     * 打开失败
     */
    FAILED,

    /**
     * 已关闭
     */
    CLOSED
}