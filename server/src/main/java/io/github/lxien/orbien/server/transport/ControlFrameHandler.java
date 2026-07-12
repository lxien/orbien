/*
 *    Copyright 2026 lxien
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package io.github.lxien.orbien.server.transport;

import com.alibaba.cola.statemachine.StateMachine;
import io.github.lxien.orbien.core.message.Message;
import io.github.lxien.orbien.core.message.TMSP;
import io.github.lxien.orbien.core.message.TMSPFrame;
import io.github.lxien.orbien.core.transport.AttributeKeys;
import io.github.lxien.orbien.core.transport.ChannelType;
import io.github.lxien.orbien.core.enums.TransportProtocol;
import io.github.lxien.orbien.core.transport.compress.CompressionType;
import io.github.lxien.orbien.core.transport.compress.TmspPayloadCompressor;
import io.github.lxien.orbien.core.utils.ChannelUtils;
import io.github.lxien.orbien.core.utils.ProtobufUtil;
import io.github.lxien.orbien.server.filetransfer.FileTransferCoordinator;
import io.github.lxien.orbien.server.statemachine.agent.*;
import io.github.lxien.orbien.server.statemachine.agent.*;
import io.github.lxien.orbien.server.statemachine.agent.command.ConnectionCreateCmd;
import io.github.lxien.orbien.server.statemachine.agent.action.CreateConnectionAction;
import io.github.lxien.orbien.server.statemachine.stream.action.StreamOpenResponseAction;
import io.github.lxien.orbien.server.statemachine.stream.*;
import io.github.lxien.orbien.server.transport.connection.DirectConnectionPool;
import io.github.lxien.orbien.server.transport.connection.MultiplexConnectionPool;
import io.github.lxien.orbien.server.statemachine.stream.*;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 控制隧道消息处理器
 *
 * @author lxien
 */
@Component
@ChannelHandler.Sharable
public class ControlFrameHandler extends SimpleChannelInboundHandler<TMSPFrame> {
    private final InternalLogger logger = InternalLoggerFactory.getInstance(ControlFrameHandler.class);
    @Autowired
    private AgentManager agentManager;
    @Autowired
    private StreamManager streamManager;

    @Autowired
    @Qualifier("agentStateMachine")
    private StateMachine<AgentState, AgentEvent, AgentContext> agentStateMachine;
    @Autowired
    private CreateConnectionAction createConnectionAction;
    @Autowired
    private MultiplexConnectionPool multiplexConnectionPool;
    @Autowired
    private DirectConnectionPool directConnectionPool;
    @Autowired
    private StreamOpenResponseAction streamOpenResponseAction;
    @Autowired
    private FileTransferCoordinator fileTransferCoordinator;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, TMSPFrame frame) {
        try {
            byte msgType = frame.getMsgType();
            Optional<AgentContext> opt = agentManager.getAgentContext(ctx.channel());
            opt.ifPresent(context -> {
                if (context.getState() != AgentState.CONNECTED) {
                    return;
                }
                context.updateActiveTime();
                context.getMissedHeartbeats().set(0);
                logger.debug("更新客户端 {} 最后激活时间", context.getAgentInfo().getAgentId());
            });
            switch (msgType) {
                case TMSP.MSG_AUTH -> {
                    ByteBuf payload = frame.getPayload();
                    Message.AuthInfo authInfo = ProtobufUtil.parseFrom(payload, Message.AuthInfo.parser());
                    ctx.channel().attr(AttributeKeys.CHANNEL_TYPE).set(ChannelType.CONTROL);
                    TransportProtocol transportProtocol = ctx.channel().attr(AttributeKeys.TRANSPORT_PROTOCOL).get();
                    logger.debug("[传输] 控制连接认证 protocol={} agentId={}",
                            transportProtocol != null ? transportProtocol.getName() : "unknown",
                            authInfo.getAgentId());
                    String agentId = authInfo.getAgentId();
                    Optional<AgentContext> contextOpt = agentManager.getAgentContext(ctx.channel());
                    if (contextOpt.isEmpty() && StringUtils.hasText(agentId)) {
                        contextOpt = agentManager.getAgentContext(agentId);
                    }
                    if (contextOpt.isPresent()) {
                        AgentContext context = contextOpt.get();
                        //如果连接是断开状态，说明是断线重连，更新连接并重试连接
                        if (context.getState() == AgentState.DISCONNECTED) {
                            logger.debug("断线重连：{}", context.getAgentId());
                            Channel oldChannel = context.getControl();
                            if (oldChannel != null && oldChannel != ctx.channel()) {
                                ChannelUtils.closeOnFlush(oldChannel);
                            }
                            agentManager.rebindControlChannel(context, ctx.channel());
                            context.setVariable(AgentConstants.AGENT_AUTH_INFO, authInfo);
                            context.fireEvent(AgentEvent.RETRY_CONNECT);
                        } else {
                            //重复登录，断开旧连接，设置新连接
                            Channel oldControl = context.getControl();
                            if (oldControl != null && oldControl != ctx.channel()) {
                                ChannelUtils.closeOnFlush(oldControl);
                            }
                            agentManager.rebindControlChannel(context, ctx.channel());
                            logger.debug("客户端 {} 重新登录", agentId);
                        }
                    } else {
                        AgentContext agentContext = agentManager.createAgent(ctx.channel(), agentStateMachine);
                        agentContext.setVariable(AgentConstants.AGENT_AUTH_INFO, authInfo);
                        agentContext.fireEvent(AgentEvent.AUTH_START);
                    }
                }
                case TMSP.MSG_GOAWAY -> {
                    logger.debug("收到停止客户端消息");
                    Optional<AgentContext> ag = agentManager.getAgentContext(frame.getStreamId());
                    ag.ifPresent(agentContext -> agentContext.fireEvent(AgentEvent.REMOTE_GOAWAY));
                }

                case TMSP.MSG_CONNECTION_CREATE -> {
                    TransportProtocol tunnelProtocol = ctx.channel().attr(AttributeKeys.TRANSPORT_PROTOCOL).get();
                    logger.debug("[传输] 收到数据隧道注册 protocol={} encrypt={} multiplex={} channelClass={}",
                            tunnelProtocol != null ? tunnelProtocol.getName() : "unknown",
                            frame.isEncrypted(), frame.isMuxTunnel(), ctx.channel().getClass().getSimpleName());
                    Optional<AgentContext> ag = agentManager.getAgentContext(frame.getStreamId());
                    if (ag.isPresent()) {
                        AgentContext agentContext = ag.get();
                        Channel control = agentContext.getControl();
                        Channel tunnel = ctx.channel();
                        if (control == tunnel) {
                            logger.error("[传输] 控制隧道与数据隧道相同，关闭连接 connectionId={}",
                                    frame.getStreamId());
                            ChannelUtils.closeOnFlush(ctx.channel());
                            return;
                        }
                        Message.CreateConnectionRequest req = ProtobufUtil.parseFrom(frame.getPayload(),
                                Message.CreateConnectionRequest.parser());
                        ConnectionCreateCmd cmd = new ConnectionCreateCmd(tunnel, frame.isEncrypted(),
                                frame.isMuxTunnel(), req.getTunnelId());
                        logger.debug("[传输] 同步注册数据隧道 agentId={} tunnelId={}",
                                agentContext.getAgentInfo().getAgentId(), req.getTunnelId());
                        createConnectionAction.registerTunnelImmediately(agentContext, cmd);
                    } else {
                        logger.warn("[传输] 未找到 agent connectionId={}，关闭数据隧道", frame.getStreamId());
                        ChannelUtils.closeOnFlush(ctx.channel());
                    }
                }
                case TMSP.MSG_PING -> {
                    logger.debug("收到来自客户端PING消息");
                    Optional<AgentContext> ag = agentManager.getAgentContext(ctx.channel());
                    if (ag.isPresent()) {
                        AgentContext agentContext = ag.get();
                        TMSPFrame pong = new TMSPFrame(0, TMSP.MSG_PONG);
                        Channel control = agentContext.getControl();
                        control.writeAndFlush(pong);
                        logger.debug("回复客户端 {} PONG 消息", agentContext.getAgentId());
                    }
                }

                //---------------------------------------Stream-------------------------------------------------//
                case TMSP.MSG_STREAM_OPEN_RESP -> {
                    int streamId = frame.getStreamId();
                    StreamContext streamContext = streamManager.getStreamContext(streamId);
                    if (streamContext == null) {
                        logger.warn("流上下文不存在 - [streamId={}]", streamId);
                        return;
                    }
                    ByteBuf payload = frame.getPayload();
                    Message.OpenStreamResponse resp = ProtobufUtil.parseFrom(payload, Message.OpenStreamResponse.parser());
                    String tunnelId = resp.getTunnelId();
                    streamContext.setMultiplex(frame.isMuxTunnel());
                    streamContext.setVariable(StreamConstants.TUNNEL_ID, tunnelId);
                    streamContext.setCompress(frame.isCompressed());
                    streamContext.setCompressAlgorithm(CompressionType.fromFlag(frame.getFlags()));
                    streamContext.setEncrypt(frame.isEncrypted());
                    streamContext.fireEvent(StreamEvent.STREAM_OPEN_SUCCESS);
                }
                case TMSP.MSG_STREAM_DATA -> {
                    int streamId = frame.getStreamId();
                    TransportProtocol tunnelProtocol = ctx.channel().attr(AttributeKeys.TRANSPORT_PROTOCOL).get();
                    TmspPayloadCompressor.ForwardPayload forward =
                            TmspPayloadCompressor.decodeForForward(ctx.channel(), frame);
                    ByteBuf decoded = forward.buf();
                    logger.debug("[传输] 收到隧道流数据 streamId={} protocol={} bytes={} channelClass={}",
                            streamId, tunnelProtocol != null ? tunnelProtocol.getName() : "unknown",
                            decoded.readableBytes(), ctx.channel().getClass().getSimpleName());
                    StreamContext streamContext = streamManager.getStreamContext(streamId);
                    StreamState state = streamContext != null ? streamContext.getState() : null;
                    if (streamContext != null
                            && (state == StreamState.OPENED || state == StreamState.PAUSED)) {
                        streamContext.forwardToRemote(decoded, forward.sharedWithInbound());
                    } else {
                        forward.releaseIfOwned();
                    }
                }
                case TMSP.MSG_STREAM_CLOSE -> {
                    logger.debug("收到来自远程关闭流消息");
                    int streamId = frame.getStreamId();
                    StreamContext streamContext = streamManager.getStreamContext(streamId);
                    if (streamContext != null) {
                        Channel tunnel = resolveDrainChannel(streamContext);
                        (tunnel != null ? tunnel.eventLoop() : ctx.channel().eventLoop()).schedule(() -> {
                            StreamContext pending = streamManager.getStreamContext(streamId);
                            if (pending != null) {
                                pending.fireEvent(StreamEvent.STREAM_REMOTE_CLOSE);
                            }
                        }, 200, TimeUnit.MILLISECONDS);
                    }
                }
                //暂停流
                case TMSP.MSG_STREAM_PAUSE -> {
                    int streamId = frame.getStreamId();
                    logger.debug("收到来自远程暂停流 {} 消息", streamId);
                    StreamContext streamContext = streamManager.getStreamContext(streamId);
                    if (streamContext != null) {
                        streamContext.fireEvent(StreamEvent.STREAM_REMOTE_PAUSE);
                    }
                }
                //恢复流 / 独立隧道客户端透传确认
                case TMSP.MSG_STREAM_RESUME -> {
                    int streamId = frame.getStreamId();
                    StreamContext streamContext = streamManager.getStreamContext(streamId);
                    if (streamContext != null && streamContext.isAwaitingClientPassthroughAck()) {
                        logger.debug("收到客户端独立隧道透传确认 streamId={}", streamId);
                        streamOpenResponseAction.onClientPassthroughReady(streamContext);
                    } else if (streamContext != null) {
                        logger.debug("收到来自远程恢复流 {} 消息", streamId);
                        streamContext.fireEvent(StreamEvent.STREAM_REMOTE_RESUME);
                    }
                }
                //代理配置上报
                case TMSP.MSG_PROXY_REPORT_REQ -> {
                    logger.debug("客户端代理配置上报");
                    agentManager.getAgentContext(ctx.channel()).ifPresent(agentContext -> {
                        if (frame.getPayload()!=null){
                            Message.BatchCreateProxiesRequest proxies = ProtobufUtil.parseFrom(
                                    frame.getPayload(),
                                    Message.BatchCreateProxiesRequest.parser());
                            agentContext.setVariable(AgentConstants.BATCH_CREATE_PROXIES_REQUEST, proxies);
                        }
                        agentContext.fireEvent(AgentEvent.PROXY_REPORT);
                    });
                }
                //代理服务节点健康状态上报
                case TMSP.MSG_SERVICE_HEALTH_REPORT -> {
                    Optional<AgentContext> agentOpt = agentManager.getAgentContext(ctx.channel());
                    if (agentOpt.isEmpty()) {
                        logger.warn("收到健康状态上报但 AgentContext 不存在，已丢弃");
                        break;
                    }
                    AgentContext agentContext = agentOpt.get();
                    if (agentContext.getState() != AgentState.CONNECTED) {
                        logger.debug("Agent 状态为 {}，忽略健康状态上报", agentContext.getState());
                        break;
                    }
                    if (frame.getPayload() == null) {
                        logger.warn("健康状态上报 payload 为空，已丢弃");
                        break;
                    }
                    logger.debug("代理服务节点健康状态上报 agentId={}", agentContext.getAgentId());
                    Message.BatchReportServiceHealthRequest healthReq = ProtobufUtil.parseFrom(
                            frame.getPayload(),
                            Message.BatchReportServiceHealthRequest.parser());
                    agentContext.setVariable(AgentConstants.BATCH_REPORT_SERVICE_HEALTH_REQUEST, healthReq);
                    agentContext.fireEvent(AgentEvent.SERVICE_HEALTH_REPORT);
                }
                case TMSP.MSG_FILE_LIST_RESP -> {
                    Message.FileListResponse resp = parseCompressedFileMessage(ctx, frame, Message.FileListResponse.parser());
                    fileTransferCoordinator.onListResp(resp);
                }
                case TMSP.MSG_FILE_OP_RESP -> {
                    Message.FileOpResponse resp = parseCompressedFileMessage(ctx, frame, Message.FileOpResponse.parser());
                    fileTransferCoordinator.onOpResp(resp);
                }
                case TMSP.MSG_FILE_TRANSFER_DONE -> {
                    Message.FileTransferDone done = parseCompressedFileMessage(ctx, frame, Message.FileTransferDone.parser());
                    fileTransferCoordinator.onTransferDone(done);
                }
                case TMSP.MSG_FILE_CHUNK -> {
                    Message.FileChunk chunk = parseCompressedFileMessage(ctx, frame, Message.FileChunk.parser());
                    fileTransferCoordinator.onChunk(chunk);
                }
            }
        } catch (Exception e) {
            logger.error(e);
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        Channel channel = ctx.channel();
        ChannelType channelType = getChannelType(channel);
        if (channelType == ChannelType.CONTROL) {
            agentManager.getAgentContext(ctx.channel()).ifPresent(agentContext -> {
                logger.debug("与客户端 {} 断开连接", agentContext.getAgentId());
                agentContext.fireEvent(AgentEvent.DISCONNECT);
            });
        } else if (channelType == ChannelType.TUNNEL) {
            logger.warn("[传输] 数据隧道断开 channelClass={}", channel.getClass().getSimpleName());
            directConnectionPool.removeByChannel(channel);
            multiplexConnectionPool.removeByChannel(channel);
            streamManager.closeStreamsByTunnel(channel);
        }
    }

    private void handleTunnelFailure(Channel channel, Throwable cause) {
        if (getChannelType(channel) != ChannelType.TUNNEL) {
            return;
        }
        logger.warn("[传输] 数据隧道异常 channelClass={}", channel.getClass().getSimpleName(), cause);
        directConnectionPool.removeByChannel(channel);
        multiplexConnectionPool.removeByChannel(channel);
        streamManager.closeStreamsByTunnel(channel);
        if (channel.isActive()) {
            ChannelUtils.closeOnFlush(channel);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        Channel channel = ctx.channel();
        ChannelType channelType = getChannelType(channel);
        if (channelType == ChannelType.CONTROL) {
            logger.error("控制连接异常: ", cause);
            agentManager.getAgentContext(ctx.channel()).ifPresent(agentContext -> {
                logger.debug("控制连接异常，触发断连: {}", agentContext.getAgentId());
                agentContext.fireEvent(AgentEvent.DISCONNECT);
            });
        } else if (channelType == ChannelType.TUNNEL) {
            handleTunnelFailure(channel, cause);
        }
    }

    @Override
    public void channelWritabilityChanged(ChannelHandlerContext ctx) {
        Channel channel = ctx.channel();
        if (getChannelType(channel) == ChannelType.TUNNEL) {
            if (channel.isWritable()) {
                Set<Integer> pausedStreamIds = streamManager.getPausedStreamIds(channel);
                if (!pausedStreamIds.isEmpty()) {
                    logger.debug("[背压] 隧道恢复可写，恢复 {} 个访客读取", pausedStreamIds.size());
                    pausedStreamIds.forEach(streamId -> {
                        StreamContext streamContext = streamManager.getStreamContext(streamId);
                        if (streamContext != null) {
                            Channel visitor = streamContext.getVisitor();
                            if (visitor != null && visitor.isActive()) {
                                visitor.config().setOption(ChannelOption.AUTO_READ, true);
                                visitor.read();
                            }
                            streamManager.removePausedStream(channel, streamId);
                        }
                    });
                }
            }
        }
    }

    private ChannelType getChannelType(Channel channel) {
        ChannelType channelType = channel.attr(AttributeKeys.CHANNEL_TYPE).get();
        return channelType != null ? channelType : ChannelType.UNKNOWN;
    }

    private Channel resolveDrainChannel(StreamContext streamContext) {
        if (streamContext.getTunnelEntry() != null) {
            return streamContext.getTunnelEntry().getChannel();
        }
        return null;
    }

    private <T extends com.google.protobuf.Message> T parseCompressedFileMessage(ChannelHandlerContext ctx,
                                                                                  TMSPFrame frame,
                                                                                  com.google.protobuf.Parser<T> parser) {
        TmspPayloadCompressor.ControlPayload payload =
                TmspPayloadCompressor.decodeControlPayload(ctx.channel(), frame);
        try {
            return ProtobufUtil.parseFrom(payload.buf(), parser);
        } finally {
            payload.releaseIfOwned();
        }
    }
}
