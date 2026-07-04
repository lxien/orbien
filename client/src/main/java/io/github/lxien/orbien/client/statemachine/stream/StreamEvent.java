package io.github.lxien.orbien.client.statemachine.stream;

public enum StreamEvent {
    /**
     * 打开流
     */
    STREAM_OPEN,

    /**
     * 打开流成功
     */
    STREAM_OPEN_SUCCESS,

    /**
     * 打开流失败
     */
    STREAM_OPEN_FAILURE,
    /**
     * 本地关闭流
     */
    STREAM_LOCAL_CLOSE,

    /**
     * 来自远程的关闭流
     */
    STREAM_REMOTE_CLOSE,
    /**
     * 本地主动流控
     */
    STREAM_LOCAL_PAUSE,
    STREAM_LOCAL_RESUME,

    /**
     * 远端通知流控
     */
    STREAM_REMOTE_PAUSE,
    STREAM_REMOTE_RESUME,
    /**
     * 收到重置流请求
     */
    STREAM_RESET,

    /**
     * 创建一个新的连接
     */
    CREATE_NEW_CONN
}