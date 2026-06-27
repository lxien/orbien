package com.xiaoniucode.etp.client.statemachine.agent.action.connection;

import com.xiaoniucode.etp.client.config.AppConfig;
import com.xiaoniucode.etp.client.statemachine.agent.AgentContext;
import com.xiaoniucode.etp.client.transport.TunnelConnectionFactory;
import com.xiaoniucode.etp.client.transport.connection.DirectPool;
import com.xiaoniucode.etp.client.transport.connection.MultiplexPool;
import com.xiaoniucode.etp.core.message.Message;
import com.xiaoniucode.etp.core.message.TMSP;
import com.xiaoniucode.etp.core.message.TMSPFrame;
import com.xiaoniucode.etp.core.transport.TunnelEntry;
import com.xiaoniucode.etp.core.utils.ProtobufUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

/**
 * 隧道连接创建辅助类
 */
public class ConnCreateHelper {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(ConnCreateHelper.class);

    /**
     * 创建多路复用隧道并发送创建请求
     */
    public static void createMultiplexTunnel(AgentContext context, AppConfig config, boolean isEncrypt) {
        TunnelConnectionFactory.createConnection(context, config, isEncrypt, tunnel -> {
            if (tunnel == null) {
                logger.error("创建多路复用连接失败");
                return;
            }

            MultiplexPool multiplexPool = context.getMultiplexPool();
            TunnelEntry tunnelEntry = multiplexPool.createChannel(isEncrypt, tunnel);

            if (tunnelEntry != null) {
                sendTunnelCreateRequest(context, tunnel, tunnelEntry, isEncrypt, true);
            }
        });
    }

    /**
     * 创建独立隧道并发送创建请求
     */
    public static void createDirectTunnel(AgentContext context, AppConfig config, boolean isEncrypt) {
        TunnelConnectionFactory.createConnection(context, config, isEncrypt, tunnel -> {
            if (tunnel == null) {
                logger.error("创建独立连接失败");
                return;
            }

            DirectPool directPool = context.getDirectPool();
            TunnelEntry tunnelEntry = directPool.createTunnel(tunnel, isEncrypt);

            if (tunnelEntry != null) {
                sendTunnelCreateRequest(context, tunnel, tunnelEntry, isEncrypt, false);
            }
        });
    }

    /**
     * 发送隧道创建请求
     */
    public static void sendTunnelCreateRequest(AgentContext context, Channel tunnel,
                                               TunnelEntry tunnelEntry, boolean isEncrypt, boolean isMultiplex) {
        Integer connectionId = context.getConnectionId();

        Message.CreateConnectionRequest body = Message.CreateConnectionRequest.newBuilder()
                .setTunnelId(tunnelEntry.getTunnelId())
                .build();

        ByteBuf payload = ProtobufUtil.toByteBuf(body, tunnel.alloc());
        TMSPFrame frame = new TMSPFrame(connectionId, TMSP.MSG_CONNECTION_CREATE, payload);
        frame.setMultiplexTunnel(isMultiplex);
        frame.setEncrypted(isEncrypt);

        tunnel.writeAndFlush(frame).addListener(f -> {
            if (f.isSuccess()) {
                logger.debug("隧道创建请求发送成功，tunnelId={}", tunnelEntry.getTunnelId());
            } else {
                logger.error("隧道创建请求发送失败，tunnelId={}", tunnelEntry.getTunnelId(), f.cause());
            }
        });
    }
}
