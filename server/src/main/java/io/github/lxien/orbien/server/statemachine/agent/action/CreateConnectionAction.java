package io.github.lxien.orbien.server.statemachine.agent.action;

import io.github.lxien.orbien.core.enums.TunnelType;
import io.github.lxien.orbien.core.message.Message;
import io.github.lxien.orbien.core.message.TMSP;
import io.github.lxien.orbien.core.message.TMSPFrame;
import io.github.lxien.orbien.core.transport.*;
import io.github.lxien.orbien.core.enums.TransportProtocol;
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
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.quic.QuicStreamChannel;
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
        registerTunnelImmediately(context, cmd);
    }

    /**
     * 在数据隧道所属 EventLoop 上同步完成入池与响应，避免 QUIC 等异步传输与控制面竞态。
     */
    public void registerTunnelImmediately(AgentContext agentContext, ConnectionCreateCmd cmd) {
        Channel tunnel = cmd.getTunnel();
        Runnable task = () -> executeRegister(agentContext, cmd);
        if (tunnel.eventLoop().inEventLoop()) {
            task.run();
        } else {
            tunnel.eventLoop().execute(task);
        }
    }

    private void executeRegister(AgentContext context, ConnectionCreateCmd cmd) {
        Channel tunnel = cmd.getTunnel();
        tunnel.attr(AttributeKeys.CHANNEL_TYPE).set(ChannelType.TUNNEL);

        String tunnelId = cmd.getTunnelId();
        boolean multiplex = cmd.isMultiplex();
        boolean encrypt = cmd.isEncrypt();
        String agentId = context.getAgentInfo().getAgentId();

        TransportProtocol protocol = tunnel.attr(AttributeKeys.TRANSPORT_PROTOCOL).get();
        if (protocol == null) {
            logger.warn("[传输] 隧道 channel 未标记协议，回退为 tcp tunnelId={} channelClass={}",
                    tunnelId, tunnel.getClass().getSimpleName());
            protocol = TransportProtocol.TCP;
        }
        final TransportProtocol tunnelProtocol = protocol;

        logger.debug("[传输] 注册数据隧道 agentId={} tunnelId={} protocol={} encrypt={} multiplex={} channelClass={} streamId={}",
                agentId, tunnelId, tunnelProtocol.getName(), encrypt, multiplex,
                tunnel.getClass().getSimpleName(), describeQuicStreamId(tunnel));

        createPool(agentId, tunnelId, protocol, multiplex, encrypt, tunnel);
        tunnel.config().setOption(ChannelOption.AUTO_READ, true);

        Message.CreateConnectionResponse resp = Message.CreateConnectionResponse.newBuilder()
                .setTunnelId(tunnelId)
                .setStatus(Message.Status.newBuilder().setCode(0))
                .build();

        ByteBuf payload = ProtobufUtil.toByteBuf(resp, tunnel.alloc());
        TMSPFrame frame = new TMSPFrame(0, TMSP.MSG_CONNECTION_CREATE_RESP, payload);
        frame.setEncrypted(encrypt);
        frame.setMultiplexTunnel(multiplex);
        if (!tunnel.isActive() || !tunnel.isWritable()) {
            logger.error("[传输] 数据隧道不可用，隧道创建结果发送失败 tunnelId={} protocol={} active={} writable={}",
                    tunnelId, tunnelProtocol.getName(), tunnel.isActive(), tunnel.isWritable());
            return;
        }
        tunnel.writeAndFlush(frame).addListener((ChannelFutureListener) future -> {
            logger.debug("[传输] 隧道创建结果已写入数据通道 tunnelId={} protocol={} refCnt={} success={}",
                    tunnelId, tunnelProtocol.getName(), payload.refCnt(), future.isSuccess());
            if (!future.isSuccess()) {
                logger.error("[传输] 隧道创建结果响应失败 tunnelId={} protocol={}",
                        tunnelId, tunnelProtocol.getName(), future.cause());
            }
        });
    }

    public void createPool(String agentId, String tunnelId, TransportProtocol protocol,
                           boolean isMultiplex, boolean isEncrypt, Channel tunnel) {
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
        logger.debug("[传输] 写入连接池 agentId={} tunnelId={} protocol={} encrypt={} multiplex={}",
                agentId, tunnelId, protocol.getName(), isEncrypt, isMultiplex);
        TunnelEntry poolEntry = new TunnelEntry(tunnelId, protocol, isEncrypt, tunnel,
                isMultiplex ? TunnelType.MULTIPLEX : TunnelType.DIRECT);
        poolEntry.setActive(true);
        if (isMultiplex) {
            PipelineConfigure.removeControlIdleCheckHandler(tunnel);
            ChannelPipeline pipeline = tunnel.pipeline();
            if (pipeline.get(NettyConstants.IDLE_CHECK_HANDLER) == null) {
                pipeline.addBefore(NettyConstants.CONTROL_FRAME_HANDLER, NettyConstants.IDLE_CHECK_HANDLER, new IdleCheckHandler());
            }
            multiplexConnectionPool.setChannel(agentId, protocol, isEncrypt, poolEntry);
        } else {
            directConnectionPool.register(agentId, poolEntry);
        }
    }

    private static Object describeQuicStreamId(Channel tunnel) {
        if (tunnel instanceof QuicStreamChannel streamChannel) {
            return streamChannel.streamId();
        }
        return "n/a";
    }
}
