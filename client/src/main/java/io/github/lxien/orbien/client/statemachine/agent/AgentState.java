package io.github.lxien.orbien.client.statemachine.agent;


public enum AgentState {

    /**
     * 初始状态：客户端对象刚创建，未开始任何操作
     */
    IDLE,

    /**
     * 正在建立连接（配置检查、SSL 初始化、TCP 握手、认证等全过程）
     */
    CONNECTING,

    /**
     * 控制通道已成功建立，可正常创建隧道和转发流量
     */
    CONNECTED,

    /**
     * 连接已断开（网络异常、心跳超时等），会触发自动重连
     */
    DISCONNECTED,

    /**
     * 连接失败或达到最大重试次数后的终态，不再自动重连
     */
    FAILED,

    /**
     * 已主动停止/关闭（收到关闭事件、GoAway 停止指令）
     */
    SHUTDOWN
}