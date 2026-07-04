package io.github.lxien.orbien.client.statemachine.agent;

/**
 * 代理客户端状态机事件枚举
 */
public enum AgentEvent {
    /**
     * 开始启动
     */
    START,
    /**
     * 配置检查完成
     */
    CONFIG_CHECKED,
    /**
     * SSL 初始化完成
     */
    SSL_INITIALIZED,
    /**
     * 尝试连接
     */
    CONNECT_ATTEMPT,
    /**
     * 连接成功
     */
    CONNECT_SUCCESS,
    /**
     * 连接失败
     */
    CONNECT_FAILURE,
    /**
     * 重试
     */
    RETRY,
    /**
     * 错误
     */
    ERROR,
    /**
     * 开始认证
     */
    AUTH_START,
    /**
     * 认证成功
     */
    AUTH_SUCCESS,
    /**
     * 认证响应
     */
    AUTH_RESPONSE,
    /**
     * 本地断开连接，清理本地客户端所有资源，通知对端断开
     */
    LOCAL_GOAWAY,
    /**
     * 来自远程的断开，清理客户端所有资源
     */
    REMOTE_GOAWAY,
    /**
     * 网络错误
     */
    NETWORK_ERROR,
    /**
     * 连接断开
     */
    DISCONNECT,
    /**
     * 创建连接池
     */
    CREATE_CONN_POOL,
    /**
     * 创建单个连接
     */
    CREATE_NEW_CONN,
    /**
     * 创建连接池响应
     */
    CREATE_CONN_POOL_RESP,

    /**
     * 代理配置上报响应
     */
    PROXY_REPORT_RESP,
    PROXY_CONFIG_SYNC,
}
