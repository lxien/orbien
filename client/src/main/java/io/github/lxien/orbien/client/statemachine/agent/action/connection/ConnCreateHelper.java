package io.github.lxien.orbien.client.statemachine.agent.action.connection;

import io.github.lxien.orbien.client.config.AppConfig;
import io.github.lxien.orbien.client.statemachine.agent.AgentContext;
import io.github.lxien.orbien.client.transport.TransportClientBootstrap;
import io.github.lxien.orbien.core.message.Message;
import io.github.lxien.orbien.core.message.TMSP;
import io.github.lxien.orbien.core.message.TMSPFrame;
import io.github.lxien.orbien.core.transport.IdleCheckHandler;
import io.github.lxien.orbien.core.transport.NettyConstants;
import io.github.lxien.orbien.core.transport.TunnelEntry;
import io.github.lxien.orbien.core.enums.TransportProtocol;
import io.github.lxien.orbien.core.utils.ProtobufUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

public final class ConnCreateHelper {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(ConnCreateHelper.class);

    private ConnCreateHelper() {
    }

    public static void createMultiplexTunnel(AgentContext context, AppConfig config,
                                             TransportProtocol protocol, boolean isEncrypt) {
        connectTunnel(context, config, protocol, isEncrypt, true);
    }

    public static void createDirectTunnel(AgentContext context, AppConfig config,
                                          TransportProtocol protocol, boolean isEncrypt) {
        connectTunnel(context, config, protocol, isEncrypt, false);
    }

    private static void connectTunnel(AgentContext context, AppConfig config,
                                      TransportProtocol protocol, boolean isEncrypt, boolean multiplex) {
        logger.debug("[传输] 创建数据隧道 protocol={} encrypt={} multiplex={}",
                protocol.getName(), isEncrypt, multiplex);
        TransportClientBootstrap.connectTunnel(
                config,
                protocol,
                isEncrypt,
                context.getControlWorkerGroup(),
                context.getTlsContext(),
                pipeline -> pipeline
                        .addLast(NettyConstants.IDLE_CHECK_HANDLER, IdleCheckHandler.forDataTunnel())
                        .addLast(NettyConstants.CONTROL_FRAME_HANDLER, context.getControlFrameHandler())
        ).whenComplete((session, error) -> {
            if (error != null) {
                logger.error("创建{}连接失败 [{}]", multiplex ? "多路复用" : "独立", protocol.getName(), error);
                return;
            }
            Channel tunnel = session.nettyChannel();
            logger.debug("[传输] 数据隧道连接成功 protocol={} channelClass={} sessionId={}",
                    protocol.getName(), tunnel.getClass().getSimpleName(), session.sessionId());
            TunnelEntry tunnelEntry = multiplex
                    ? context.getPoolManager().createMultiplex(protocol, isEncrypt, tunnel)
                    : context.getPoolManager().createDirect(protocol, isEncrypt, tunnel);
            if (tunnelEntry != null) {
                sendTunnelCreateRequest(context, tunnel, tunnelEntry, protocol, isEncrypt, multiplex);
            }
        });
    }

    public static void sendTunnelCreateRequest(AgentContext context, Channel tunnel,
                                               TunnelEntry tunnelEntry, TransportProtocol protocol,
                                               boolean isEncrypt, boolean isMultiplex) {
        Integer connectionId = context.getConnectionId();
        Message.CreateConnectionRequest body = Message.CreateConnectionRequest.newBuilder()
                .setTunnelId(tunnelEntry.getTunnelId())
                .build();
        ByteBuf payload = ProtobufUtil.toByteBuf(body, tunnel.alloc());
        TMSPFrame frame = new TMSPFrame(connectionId, TMSP.MSG_CONNECTION_CREATE, payload);
        frame.setMultiplexTunnel(isMultiplex);
        frame.setEncrypted(isEncrypt);
        tunnel.attr(io.github.lxien.orbien.core.transport.AttributeKeys.TRANSPORT_PROTOCOL).set(protocol);
        tunnel.config().setOption(io.netty.channel.ChannelOption.AUTO_READ, true);
        tunnel.writeAndFlush(frame).addListener(f -> {
            if (f.isSuccess()) {
                logger.debug("[传输] 隧道创建请求已发送 tunnelId={} protocol={} encrypt={} multiplex={} channelClass={}",
                        tunnelEntry.getTunnelId(), protocol.getName(), isEncrypt, isMultiplex,
                        tunnel.getClass().getSimpleName());
            } else {
                logger.error("[传输] 隧道创建请求发送失败 tunnelId={} protocol={} channelClass={}",
                        tunnelEntry.getTunnelId(), protocol.getName(), tunnel.getClass().getSimpleName(), f.cause());
            }
        });
    }
}
