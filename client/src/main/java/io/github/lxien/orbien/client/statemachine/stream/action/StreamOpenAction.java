package io.github.lxien.orbien.client.statemachine.stream.action;

import io.github.lxien.orbien.client.statemachine.agent.AgentContext;
import io.github.lxien.orbien.client.statemachine.agent.AgentEvent;
import io.github.lxien.orbien.client.statemachine.agent.command.ConnCreateCommand;
import io.github.lxien.orbien.client.statemachine.stream.*;
import io.github.lxien.orbien.client.statemachine.stream.StreamConstants;
import io.github.lxien.orbien.client.statemachine.stream.StreamContext;
import io.github.lxien.orbien.client.statemachine.stream.StreamEvent;
import io.github.lxien.orbien.core.codec.NewStreamCodec;
import io.github.lxien.orbien.core.message.Message;
import io.github.lxien.orbien.core.transport.AttributeKeys;
import io.github.lxien.orbien.core.message.TMSP;
import io.github.lxien.orbien.core.message.TMSPFrame;
import io.github.lxien.orbien.core.transport.compress.TmspPayloadCompressor;
import io.github.lxien.orbien.client.transport.bridge.TunnelBridgeFactory;
import io.github.lxien.orbien.client.transport.TransportProtocolResolver;
import io.github.lxien.orbien.core.transport.api.TransportEndpointResolver;
import io.github.lxien.orbien.core.enums.TransportProtocol;
import io.github.lxien.orbien.core.transport.TunnelBridge;
import io.github.lxien.orbien.core.transport.TunnelEntry;
import io.github.lxien.orbien.core.utils.ProtobufUtil;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.util.concurrent.TimeUnit;

public class StreamOpenAction extends StreamBaseAction {
    private final InternalLogger logger = InternalLoggerFactory.getInstance(StreamOpenAction.class);

    /**
     * 最大重试次数
     */
    private static final int MAX_RETRY_COUNT = 3;

    /**
     * 每次重试等待时间（毫秒）
     */
    private static final long RETRY_INTERVAL_MS = 500;

    @Override
    protected void doExecute(StreamState from, StreamState to, StreamEvent event, StreamContext streamContext) {
        logger.debug("开始打开流 {}", streamContext.getStreamId());
        if (streamContext.hasVariable(StreamConstants.VISIT_INFO)) {
            AgentContext agentContext = (AgentContext) streamContext.getAgentContext();
            Channel control = agentContext.getControl();
            int streamId = streamContext.getStreamId();

            NewStreamCodec.NewStreamInfo visitorInfo = streamContext.getAndRemoveAs(StreamConstants.VISIT_INFO, NewStreamCodec.NewStreamInfo.class);
            String localIp = visitorInfo.getLocalIp();
            int localPort = visitorInfo.getLocalPort();

            streamContext.setLocalIp(localIp);
            streamContext.setLocalPort(localPort);

            TransportProtocol transportProtocol = visitorInfo.getTransportProtocol() != null
                    ? visitorInfo.getTransportProtocol()
                    : TransportProtocolResolver.globalDefault(agentContext.getConfig());

            streamContext.setTransportProtocol(transportProtocol);
            boolean multiplex = TransportEndpointResolver.normalizeMultiplex(
                    transportProtocol, streamContext.isMultiplex());
            streamContext.setMultiplex(multiplex);
            logger.debug("[传输] 流 {} 解析数据隧道协议={} encrypt={} multiplex={} target={}:{}",
                    streamContext.getStreamId(), transportProtocol.getName(),
                    streamContext.isEncrypt(), multiplex, localIp, localPort);
            //UDP协议内网服务
            if (streamContext.isDatagram()) {
                openDatagramStream(streamContext, agentContext, control, streamId);
                return;
            }
            //TCP协议内网服务
            Bootstrap serverBootstrap = agentContext.getServerBootstrap();
            serverBootstrap.connect(localIp, localPort).addListener((ChannelFutureListener) serverFuture -> {
                if (serverFuture.isSuccess()) {
                    logger.debug("成功连接到TCP协议目标服务 - [地址={}，端口={}]", localIp, localPort);
                    Channel server = serverFuture.channel();
                    server.config().setOption(ChannelOption.AUTO_READ, false);
                    server.attr(AttributeKeys.STREAM_ID).set(streamId);

                    TunnelEntry tunnelEntry = getOrCreateTunnel(streamContext);

                    if (tunnelEntry == null) {
                        logger.error("没有可用连接，关闭流 {}", streamId);
                        streamContext.fireEvent(StreamEvent.STREAM_LOCAL_CLOSE);
                        return;
                    }

                    if (!control.isActive()) {
                        logger.error("控制连接不可用，关闭流 {} ", streamContext.getStreamId());
                        streamContext.fireEvent(StreamEvent.STREAM_LOCAL_CLOSE);
                    }

                    streamContext.setServer(server);
                    streamContext.setTunnelEntry(tunnelEntry);

                    Integer connectionId = agentContext.getConnectionId();
                    Message.OpenStreamResponse response = Message.OpenStreamResponse.newBuilder()
                            .setStatus(Message.Status.newBuilder().setCode(0).build())
                            .setConnectionId(connectionId)
                            .setTunnelId(tunnelEntry.getTunnelId())
                            .build();
                    ByteBuf payload = ProtobufUtil.toByteBuf(response, control.alloc());
                    TMSPFrame frame = new TMSPFrame(streamId, TMSP.MSG_STREAM_OPEN_RESP, payload);
                    TmspPayloadCompressor.applyStreamFlags(frame, streamContext.resolveCompressAlgorithm());
                    frame.setEncrypted(streamContext.isEncrypt());
                    frame.setMultiplexTunnel(streamContext.isMultiplex());
                    control.writeAndFlush(frame).addListener(f -> {
                        if (!f.isSuccess()) {
                            streamContext.fireEvent(StreamEvent.STREAM_LOCAL_CLOSE);
                            return;
                        }
                        if (streamContext.isMultiplex()) {
                            TunnelBridge tunnelBridge = TunnelBridgeFactory.buildMux(streamContext);
                            logger.debug("流打开成功 - [隧道类型=多路复用， 目标地址={}，目标端口={}]", localIp, localPort);
                            tunnelBridge.openAsync().addListener(openFuture -> {
                                if (!openFuture.isSuccess()) {
                                    logger.error("多路复用隧道打开失败 streamId={}", streamId, openFuture.cause());
                                    streamContext.fireEvent(StreamEvent.STREAM_LOCAL_CLOSE);
                                    return;
                                }
                                streamContext.setTunnelBridge(tunnelBridge);
                                streamContext.fireEvent(StreamEvent.STREAM_OPEN_SUCCESS);
                                tunnelEntry.getChannel().config().setOption(ChannelOption.AUTO_READ, true);
                                server.config().setOption(ChannelOption.AUTO_READ, true);
                                server.read();
                            });
                        } else {
                            TunnelBridge tunnelBridge = TunnelBridgeFactory.buildDirect(streamContext);
                            streamContext.setTunnelBridge(tunnelBridge);
                            logger.debug("独立隧道 OPEN_RESP 已发送，等待服务端透传就绪 streamId={} target={}:{}",
                                    streamId, localIp, localPort);
                        }
                    });

                } else {
                    streamContext.fireEvent(StreamEvent.STREAM_LOCAL_CLOSE);
                    logger.error("流打开失败 - [服务地址={}:服务端口={}] 不可用! 关闭流", localIp, localPort);
                    logger.error(serverFuture.cause());
                }
            });
        }
    }

    /**
     * 获取或创建隧道
     * 先尝试获取，如果没有则触发创建并等待重试
     */
    private TunnelEntry getOrCreateTunnel(StreamContext context) {
        AgentContext agentContext = (AgentContext) context.getAgentContext();
        //根据不同的传输协议获取数据连接
        TransportProtocol protocol = context.getTransportProtocol();
        TunnelEntry tunnelEntry = agentContext.getConn(protocol, context.isEncrypt(), context.isMultiplex());
        if (tunnelEntry != null) {
            logger.debug("[传输] 流 {} 命中连接池 protocol={} tunnelId={}",
                    context.getStreamId(), protocol.getName(), tunnelEntry.getTunnelId());
            return tunnelEntry;
        }

        logger.debug("[传输] 流 {} 无可用隧道，创建新连接 protocol={} encrypt={} multiplex={}",
                context.getStreamId(), protocol.getName(), context.isEncrypt(), context.isMultiplex());

        ConnCreateCommand connCreateCommand;
        if (context.isMultiplex()) {
            connCreateCommand = ConnCreateCommand.ofMultiplex(protocol, context.isEncrypt());
        } else {
            connCreateCommand = ConnCreateCommand.ofDirect(protocol, context.isEncrypt(), 1);
        }
        agentContext.setVariable("create_conn_command", connCreateCommand);
        agentContext.fireEvent(AgentEvent.CREATE_NEW_CONN);

        // 等待后重试获取
        for (int i = 1; i <= MAX_RETRY_COUNT; i++) {
            try {
                TimeUnit.MILLISECONDS.sleep(RETRY_INTERVAL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.error("等待被中断", e);
                return null;
            }

            tunnelEntry = agentContext.getConn(protocol, context.isEncrypt(), context.isMultiplex());
            if (tunnelEntry != null) {
                logger.debug("第 {} 次重试后获取到隧道", i);
                return tunnelEntry;
            }

            logger.debug("第 {} 次重试仍未获取到隧道，继续等待", i);
        }

        logger.error("重试 {} 次后仍未获取到可用连接", MAX_RETRY_COUNT);
        return null;
    }

    private void openDatagramStream(StreamContext streamContext,
                                    AgentContext agentContext,
                                    Channel control,
                                    int streamId) {
        Bootstrap udpBootstrap = agentContext.getUdpServerBootstrap();
        udpBootstrap.bind(0).addListener((ChannelFutureListener) bindFuture -> {
            if (!bindFuture.isSuccess()) {
                logger.error("UDP 流打开失败 - 无法绑定本地端口，streamId={}", streamId, bindFuture.cause());
                streamContext.fireEvent(StreamEvent.STREAM_LOCAL_CLOSE);
                return;
            }
            Channel server = bindFuture.channel();
            server.attr(AttributeKeys.STREAM_ID).set(streamId);

            TunnelEntry tunnelEntry = getOrCreateTunnel(streamContext);
            if (tunnelEntry == null) {
                logger.error("没有可用连接，关闭 UDP 流 {}", streamId);
                streamContext.fireEvent(StreamEvent.STREAM_LOCAL_CLOSE);
                return;
            }
            if (!control.isActive()) {
                logger.error("控制连接不可用，关闭 UDP 流 {}", streamId);
                streamContext.fireEvent(StreamEvent.STREAM_LOCAL_CLOSE);
                return;
            }

            streamContext.setServer(server);
            streamContext.setTunnelEntry(tunnelEntry);

            Integer connectionId = agentContext.getConnectionId();
            Message.OpenStreamResponse response = Message.OpenStreamResponse.newBuilder()
                    .setStatus(Message.Status.newBuilder().setCode(0).build())
                    .setConnectionId(connectionId)
                    .setTunnelId(tunnelEntry.getTunnelId())
                    .build();
            ByteBuf payload = ProtobufUtil.toByteBuf(response, control.alloc());
            TMSPFrame frame = new TMSPFrame(streamId, TMSP.MSG_STREAM_OPEN_RESP, payload);
            TmspPayloadCompressor.applyStreamFlags(frame, streamContext.resolveCompressAlgorithm());
            frame.setEncrypted(streamContext.isEncrypt());
            frame.setMultiplexTunnel(true);
            frame.setDatagram(true);
            control.writeAndFlush(frame).addListener(f -> {
                if (f.isSuccess()) {
                    TunnelBridge tunnelBridge = TunnelBridgeFactory.buildMux(streamContext);
                    tunnelBridge.openAsync().addListener(openFuture -> {
                        if (!openFuture.isSuccess()) {
                            streamContext.fireEvent(StreamEvent.STREAM_LOCAL_CLOSE);
                            return;
                        }
                        streamContext.setTunnelBridge(tunnelBridge);
                        streamContext.fireEvent(StreamEvent.STREAM_OPEN_SUCCESS);
                        tunnelEntry.getChannel().config().setOption(ChannelOption.AUTO_READ, true);
                        logger.debug("UDP 流打开成功 - [目标地址={}，目标端口={}]", streamContext.getLocalIp(), streamContext.getLocalPort());
                    });
                } else {
                    streamContext.fireEvent(StreamEvent.STREAM_LOCAL_CLOSE);
                }
            });
        });
    }
}
