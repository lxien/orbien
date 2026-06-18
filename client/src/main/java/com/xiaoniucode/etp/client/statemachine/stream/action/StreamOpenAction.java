package com.xiaoniucode.etp.client.statemachine.stream.action;

import com.xiaoniucode.etp.client.statemachine.agent.AgentContext;
import com.xiaoniucode.etp.client.statemachine.agent.AgentEvent;
import com.xiaoniucode.etp.client.statemachine.agent.command.CreateConnCommand;
import com.xiaoniucode.etp.client.statemachine.stream.*;
import com.xiaoniucode.etp.client.transport.bridge.TunnelBridgeFactory;
import com.xiaoniucode.etp.core.codec.NewStreamCodec;
import com.xiaoniucode.etp.core.message.Message;
import com.xiaoniucode.etp.core.transport.AttributeKeys;
import com.xiaoniucode.etp.core.message.TMSP;
import com.xiaoniucode.etp.core.message.TMSPFrame;
import com.xiaoniucode.etp.core.transport.TunnelBridge;
import com.xiaoniucode.etp.core.transport.TunnelEntry;
import com.xiaoniucode.etp.core.utils.ProtobufUtil;
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

            Bootstrap serverBootstrap = agentContext.getServerBootstrap();
            serverBootstrap.connect(localIp, localPort).addListener((ChannelFutureListener) serverFuture -> {
                if (serverFuture.isSuccess()) {
                    logger.debug("成功连接到目标服务 - [地址={}，端口={}]", localIp, localPort);
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
                    Message.StreamOpenResponse req = Message.StreamOpenResponse.newBuilder()
                            .setCode(0)
                            .setConnectionId(connectionId)
                            .setTunnelId(tunnelEntry.getTunnelId())
                            .build();
                    ByteBuf payload = ProtobufUtil.toByteBuf(req, control.alloc());
                    TMSPFrame frame = new TMSPFrame(streamId, TMSP.MSG_STREAM_OPEN_RESP, payload);
                    frame.setCompressed(streamContext.isCompress());
                    frame.setEncrypted(streamContext.isEncrypt());
                    frame.setMultiplexTunnel(streamContext.isMultiplex());
                    control.writeAndFlush(frame).addListener(f -> {
                        if (f.isSuccess()) {
                            TunnelBridge tunnelBridge;
                            if (streamContext.isMultiplex()) {
                                tunnelBridge = TunnelBridgeFactory.buildMux(streamContext);
                                logger.debug("流打开成功 - [隧道类型=多路复用， 目标地址={}，目标端口={}]", localIp, localPort);
                            } else {
                                tunnelBridge = TunnelBridgeFactory.buildDirect(streamContext);
                                logger.debug("流打开成功 - [隧道类型=独立连接， 目标地址={}，目标端口={}]", localIp, localPort);
                            }
                            tunnelBridge.open();
                            streamContext.setTunnelBridge(tunnelBridge);
                            streamContext.fireEvent(StreamEvent.STREAM_OPEN_SUCCESS);
                            tunnelEntry.getChannel().config().setOption(ChannelOption.AUTO_READ, true);
                            server.config().setOption(ChannelOption.AUTO_READ, true);
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

        TunnelEntry tunnelEntry = agentContext.getConn(context.isEncrypt(), context.isMultiplex());
        if (tunnelEntry != null) {
            return tunnelEntry;
        }

        logger.debug("没有可用连接，开始创建新隧道");

        CreateConnCommand createConnCommand;
        if (context.isMultiplex()) {
            createConnCommand = CreateConnCommand.ofMultiplex(context.isEncrypt());
        } else {
            createConnCommand = CreateConnCommand.ofDirect(context.isEncrypt(), 1);
        }
        agentContext.setVariable("create_conn_command", createConnCommand);
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

            tunnelEntry = agentContext.getConn(context.isEncrypt(), context.isMultiplex());
            if (tunnelEntry != null) {
                logger.debug("第 {} 次重试后获取到隧道", i);
                return tunnelEntry;
            }

            logger.debug("第 {} 次重试仍未获取到隧道，继续等待", i);
        }

        logger.error("重试 {} 次后仍未获取到可用连接", MAX_RETRY_COUNT);
        return null;
    }
}
