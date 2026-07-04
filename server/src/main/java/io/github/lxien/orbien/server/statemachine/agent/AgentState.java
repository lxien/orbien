package io.github.lxien.orbien.server.statemachine.agent;


public enum AgentState {
    /**
     * 新建会话：服务端刚刚 accept TCP 连接，但客户端还未开始认证
     */
    NEW,
    
    /**
     * 认证中：正在进行 Token 校验、版本协商、权限检查等
     */
    AUTHENTICATING,
    
    /**
     * 已认证并活跃：控制通道正常，客户端可创建隧道、接收指令
     */
    CONNECTED,
    
    /**
     * 连接断开：客户端网络异常、心跳超时等
     * 服务端通常保留会话一段时间等待重连（重连窗口）
     */
    DISCONNECTED,
    
    /**
     * 失败终态：认证失败、协议错误、恶意行为、达到最大重试次数等
     * 服务端会立即清理资源，不再接受重连
     */
    FAILED,
    
    /**
     * 已主动关闭：服务端主动踢下线、收到客户端 GoAway、管理员手动停止等
     * 会进行资源释放、会话注销等
     */
    CLOSED
}