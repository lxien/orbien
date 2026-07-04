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
package io.github.lxien.orbien.client.transport;

import io.github.lxien.orbien.client.statemachine.agent.AgentContext;
import io.github.lxien.orbien.client.statemachine.ContextConstants;
import io.github.lxien.orbien.client.statemachine.agent.AgentEvent;
import io.github.lxien.orbien.client.statemachine.stream.*;
import io.github.lxien.orbien.core.codec.NewStreamCodec;
import io.github.lxien.orbien.core.message.Message;
import io.github.lxien.orbien.core.message.TMSP;
import io.github.lxien.orbien.core.message.TMSPFrame;
import io.github.lxien.orbien.core.utils.ProtobufUtil;
import io.github.lxien.orbien.client.statemachine.stream.*;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.io.IOException;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author lxien
 */
@ChannelHandler.Sharable
public class ControlFrameHandler extends SimpleChannelInboundHandler<TMSPFrame> {
    private final InternalLogger logger = InternalLoggerFactory.getInstance(ControlFrameHandler.class);
    private final AgentContext agentContext;

    public ControlFrameHandler(AgentContext agentContext) {
        this.agentContext = agentContext;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        ctx.fireChannelActive();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, TMSPFrame frame) {
        byte msgType = frame.getMsgType();
        agentContext.updateActiveTime();
        agentContext.getMissedHeartbeats().set(0);
        switch (msgType) {
            //********************Agent***********************//
            case TMSP.MSG_AUTH_RESP: {
                Message.AuthResponse authResponse = ProtobufUtil.parseFrom(frame.getPayload(), Message.AuthResponse.parser());
                agentContext.setVariable(ContextConstants.AUTH_RESP, authResponse);
                agentContext.fireEvent(AgentEvent.AUTH_RESPONSE);
                break;
            }
            case TMSP.MSG_PROXY_REPORT_RESP: {
                if (frame.getPayload() != null) {
                    Message.BatchCreateProxiesResponse proxies = ProtobufUtil.parseFrom(frame.getPayload(),
                            Message.BatchCreateProxiesResponse.parser());
                    agentContext.setVariable(ContextConstants.BATCH_CREATE_PROXIES_RESP, proxies);
                }
                agentContext.fireEvent(AgentEvent.PROXY_REPORT_RESP);
                break;
            }
            case TMSP.MSG_GOAWAY: {
                logger.debug("收到停止消息，准备停止客户端");
                agentContext.fireEvent(AgentEvent.REMOTE_GOAWAY);
                break;
            }
            case TMSP.MSG_ERROR: {
                Message.Error error = ProtobufUtil.parseFrom(frame.getPayload(), Message.Error.parser());
                agentContext.setVariable(ContextConstants.ERROR, error);
                agentContext.fireEvent(AgentEvent.ERROR);
                break;
            }
            case TMSP.MSG_PONG: {
                logger.debug("收到来自服务端的 PONG 消息");
                break;
            }
            //配置同步
            case TMSP.MSG_CONFIG_SYNC: {
                agentContext.fireEvent(AgentEvent.PROXY_CONFIG_SYNC);
                break;

            }
            //********************Tunnel***********************//
            case TMSP.MSG_CONNECTION_CREATE_RESP: {
                ByteBuf payload = frame.getPayload();
                Message.CreateConnectionResponse resp = ProtobufUtil.parseFrom(payload, Message.CreateConnectionResponse.parser());
                String tunnelId = resp.getTunnelId();
                agentContext.getControl().eventLoop().execute(() -> {
                    agentContext.setVariable(ContextConstants.TUNNEL_ID, tunnelId);
                    agentContext.setVariable(ContextConstants.COMPRESS, frame.isCompressed());
                    agentContext.setVariable(ContextConstants.ENCRYPT, frame.isEncrypted());
                    agentContext.setVariable(ContextConstants.MULTIPLEX, frame.isMuxTunnel());
                    agentContext.setVariable(ContextConstants.CREATE_CONN_RESP, resp);
                    agentContext.fireEvent(AgentEvent.CREATE_CONN_POOL_RESP);
                });

                break;
            }

            //********************Stream***********************//
            case TMSP.MSG_STREAM_OPEN: {
                NewStreamCodec.NewStreamInfo visitorInfo = NewStreamCodec.decode(frame.getPayload());
                StreamContext streamContext = StreamManager.createStreamContext(frame.getStreamId(), agentContext);
                streamContext.setVariable(StreamConstants.VISIT_INFO, visitorInfo);
                streamContext.setCompress(frame.isCompressed());
                streamContext.setEncrypt(frame.isEncrypted());
                streamContext.setMultiplex(frame.isMuxTunnel());
                streamContext.setDatagram(frame.isDatagram());
                streamContext.setAgentContext(agentContext);
                streamContext.fireEvent(StreamEvent.STREAM_OPEN);
                break;
            }
            case TMSP.MSG_STREAM_DATA: {
                int streamId = frame.getStreamId();
                StreamManager.getStreamContext(streamId).ifPresent(streamContext -> {
                    if (streamContext.getState() == StreamState.OPENED) {
                        streamContext.forwardToLocal(frame.getPayload());
                    }
                });
                break;
            }
            case TMSP.MSG_STREAM_CLOSE: {
                logger.debug("收到来自远程关闭流消息");
                StreamManager.getStreamContext(frame.getStreamId())
                        .ifPresent(streamContext -> {
                            streamContext.fireEvent(StreamEvent.STREAM_REMOTE_CLOSE);
                        });
                break;
            }
            //暂停流
            case TMSP.MSG_STREAM_PAUSE: {
                int streamId = frame.getStreamId();
                logger.debug("收到来自远程暂停流 {} 消息", streamId);
                StreamManager.getStreamContext(streamId)
                        .ifPresent(streamContext -> {
                            streamContext.fireEvent(StreamEvent.STREAM_REMOTE_PAUSE);
                        });
                break;
            }
            //恢复流
            case TMSP.MSG_STREAM_RESUME: {
                int streamId = frame.getStreamId();
                logger.debug("收到来自远程恢复流 {} 消息", streamId);
                StreamManager.getStreamContext(streamId)
                        .ifPresent(streamContext -> {
                            streamContext.fireEvent(StreamEvent.STREAM_REMOTE_RESUME);
                        });
                break;
            }
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        //如果是控制连接断开，需要进行重连
        if (agentContext.getControl() == ctx.channel()) {
            logger.warn("与服务器断开连接");
            agentContext.fireEvent(AgentEvent.DISCONNECT);
        } else {
            logger.error("数据连接断开：channel-{}", ctx.channel().id());
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        Channel channel = ctx.channel();
        //控制隧道
        if (channel == agentContext.getControl()) {
            if (isNetworkException(cause)) {
                if (agentContext.getControl() == ctx.channel()) {
                    logger.error("控制隧道网络错误", cause);
                    agentContext.fireEvent(AgentEvent.NETWORK_ERROR);
                }
            }
        } else {
            logger.error("数据连接异常，关闭数据连接", cause);
            //数据隧道
            // ChannelUtils.closeOnFlush(channel);
        }
        logger.error(cause.getMessage(), cause);
    }

    private boolean isNetworkException(Throwable cause) {
        if (cause instanceof IOException) {
            return true;
        }
        if (cause.getMessage() != null) {
            String msg = cause.getMessage().toLowerCase();
            return msg.contains("reset by peer") ||
                    msg.contains("connection refused") ||
                    msg.contains("network is unreachable") ||
                    msg.contains("timeout");
        }
        return false;
    }

    @Override
    public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
        logger.debug("隧道可写性发生变化：{}", ctx.channel().isWritable());
        Channel tunnel = ctx.channel();
        if (tunnel == agentContext.getControl()) {
            logger.debug("控制隧道可写性发生变化，暂不处理");
            return;
        }
        boolean writable = tunnel.isWritable();
        if (writable) {
            //数据隧道恢复可写，恢复暂停的从服务器读取
            Set<Integer> pausedStreamIds = StreamManager.getPausedStreamIds(tunnel);
            if (!pausedStreamIds.isEmpty()) {
                logger.debug("控制隧道恢复可写，恢复 {} 个访问者读取", pausedStreamIds.size());
                pausedStreamIds.forEach(streamId -> {
                    Optional<StreamContext> streamContextOpt = StreamManager.getStreamContext(streamId);
                    if (streamContextOpt.isPresent()) {
                        StreamContext streamContext = streamContextOpt.get();
                        Channel server = streamContext.getServer();
                        if (server != null) {
                            ctx.executor().schedule(() -> {
                                server.config().setOption(ChannelOption.AUTO_READ, true);
                                server.read();
                                StreamManager.removePausedStream(tunnel, streamId);
                            }, 5, TimeUnit.MILLISECONDS);
                        }
                    }
                });
            }
        }
        super.channelWritabilityChanged(ctx);
    }
}



