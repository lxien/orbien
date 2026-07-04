package io.github.lxien.orbien.server.statemachine.agent.action;

import io.github.lxien.orbien.core.enums.TunnelType;
import io.github.lxien.orbien.core.message.Message;
import io.github.lxien.orbien.core.message.TMSP;
import io.github.lxien.orbien.core.message.TMSPFrame;
import io.github.lxien.orbien.core.transport.*;
import io.github.lxien.orbien.core.utils.ProtobufUtil;
import io.github.lxien.orbien.server.statemachine.agent.AgentConstants;
import io.github.lxien.orbien.server.statemachine.agent.AgentContext;
import io.github.lxien.orbien.server.statemachine.agent.AgentEvent;
import io.github.lxien.orbien.server.statemachine.agent.AgentState;
import io.github.lxien.orbien.server.statemachine.agent.command.ConnectionCreateCmd;
import io.github.lxien.orbien.server.transport.connection.DirectConnectionPool;
import io.github.lxien.orbien.server.transport.connection.MultiplexConnectionPool;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelPipeline;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class CreateConnectionAction extends AgentBaseAction {
    private final InternalLogger logger = InternalLoggerFactory.getInstance(CreateConnectionAction.class);
    @Autowired
    private DirectConnectionPool directConnectionPool;
    @Autowired
    private MultiplexConnectionPool multiplexConnectionPool;

    @Override
    protected void doExecute(AgentState from, AgentState to, AgentEvent event, AgentContext context) {
        logger.debug("开始建立连接隧道");
        ConnectionCreateCmd cmd = context.getAndRemoveAs(AgentConstants.TUNNEL_CREATE_CMD, ConnectionCreateCmd.class);
        Channel tunnel = cmd.getTunnel();
        tunnel.attr(AttributeKeys.CHANNEL_TYPE).set(ChannelType.TUNNEL);

        String tunnelId = cmd.getTunnelId();
        boolean multiplex = cmd.isMultiplex();
        boolean encrypt = cmd.isEncrypt();

        String agentId = context.getAgentInfo().getAgentId();
        createPool(agentId, tunnelId, multiplex, encrypt, tunnel);
        Channel control = context.getControl();
        Message.CreateConnectionResponse resp = Message.CreateConnectionResponse.newBuilder()
                .setTunnelId(tunnelId)
                .setStatus(Message.Status.newBuilder().setCode(0))
                .build();

        ByteBuf payload = ProtobufUtil.toByteBuf(resp, control.alloc());
        TMSPFrame frame = new TMSPFrame(0, TMSP.MSG_CONNECTION_CREATE_RESP, payload);
        frame.setEncrypted(encrypt);
        frame.setMultiplexTunnel(multiplex);
        if (!control.isActive() || !control.isWritable()) {
            logger.error("控制通道不可用，隧道创建结果结果发送失败");
            return;
        }
        control.writeAndFlush(frame).addListener((ChannelFutureListener) future -> {
            logger.debug("隧道创建结果响应引用计数：{}", payload.refCnt());
            if (!future.isSuccess()) {
                logger.error("隧道创建结果响应失败", future.cause());
            }
        });
    }

    public void createPool(String agentId, String tunnelId, boolean isMultiplex, boolean isEncrypt, Channel tunnel) {
        if (tunnel == null) {
            throw new IllegalArgumentException("tunnel 不能为空");
        }
        if (agentId == null) {
            throw new IllegalArgumentException("agentId 不能为空");
        }
        if (tunnelId == null) {
            throw new IllegalArgumentException("tunnelId 不能为空");
        }
        if (!tunnel.isActive()) {
            throw new IllegalArgumentException("连接不可用，连接池创建失败");
        }
        logger.debug("创建隧道 客户端ID={} 隧道ID={} 加密={} 多路复用={}", agentId, tunnelId, isEncrypt, isMultiplex);
        // NettyBatchWriteQueue writeQueue = NettyBatchWriteQueue.createWriteQueue(tunnel);
        TunnelEntry poolEntry = new TunnelEntry(tunnelId, isEncrypt, tunnel, isMultiplex ? TunnelType.MULTIPLEX : TunnelType.DIRECT);
        poolEntry.setActive(true);
        if (isMultiplex) {
            PipelineConfigure.removeControlIdleCheckHandler(tunnel);
            ChannelPipeline pipeline = tunnel.pipeline();
            if (pipeline.get(NettyConstants.IDLE_CHECK_HANDLER) == null) {
                pipeline.addBefore(NettyConstants.CONTROL_FRAME_HANDLER, NettyConstants.IDLE_CHECK_HANDLER, new IdleCheckHandler());
            }
            multiplexConnectionPool.setChannel(agentId, isEncrypt, poolEntry);
        } else {
            directConnectionPool.register(agentId, poolEntry);
        }
    }
}
