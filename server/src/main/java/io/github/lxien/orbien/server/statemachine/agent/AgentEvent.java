package io.github.lxien.orbien.server.statemachine.agent;

public enum AgentEvent {
    /**
     * 开始认证
     */
    AUTH_START,

    /**
     * 认证成功
     */
    AUTH_SUCCESS,

    /**
     * 认证失败
     */
    AUTH_FAILURE,

    /**
     * 标记连接断开，并不会清理资源
     */
    DISCONNECT,

    /**
     * 创建隧道
     */
    CREATE_TUNNEL,

    /**
     * 心跳超时
     */
    HEARTBEAT_TIMEOUT,
    /**
     * 本地断开连接，清理本地客户端所有资源，通知对端断开
     */
    LOCAL_GOAWAY,
    /**
     * 来自远程的断开
     * 清理客户端所有资源
     */
    REMOTE_GOAWAY,

    /**
     * 重连窗口超时
     */
    RETRY_TIMEOUT,
    /**
     * 重连
     */
    RETRY_CONNECT,

    /**
     * 代理配置上报
     */
    PROXY_REPORT,
    /**
     * 初始化新注册客户端
     */
    AGENT_INIT,
    /**
     * 代理真实服务健康状态上报
     */
    SERVICE_HEALTH_REPORT
}