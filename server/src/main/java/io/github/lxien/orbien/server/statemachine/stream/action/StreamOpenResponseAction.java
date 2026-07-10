package io.github.lxien.orbien.server.statemachine.stream.action;

import io.github.lxien.orbien.core.message.TMSP;
import io.github.lxien.orbien.core.message.TMSPFrame;
import io.github.lxien.orbien.core.transport.AttributeKeys;
import io.github.lxien.orbien.core.transport.TunnelEntry;
import io.github.lxien.orbien.core.transport.direct.DirectTunnelLifecycle;
import io.github.lxien.orbien.server.inspector.HttpCaptureRecord;
import io.github.lxien.orbien.server.inspector.HttpStreamCapture;
import io.github.lxien.orbien.server.inspector.InspectorBuffer;
import io.github.lxien.orbien.server.inspector.InspectorProperties;
import io.github.lxien.orbien.server.metrics.MetricsCollector;
import io.github.lxien.orbien.server.statemachine.agent.AgentInfo;
import io.github.lxien.orbien.server.loadbalance.LeastConnHooks;
import io.github.lxien.orbien.server.statemachine.agent.AgentContext;
import io.github.lxien.orbien.server.statemachine.stream.StreamConstants;
import io.github.lxien.orbien.server.statemachine.stream.StreamContext;
import io.github.lxien.orbien.server.statemachine.stream.StreamEvent;
import io.github.lxien.orbien.server.statemachine.stream.StreamManager;
import io.github.lxien.orbien.server.statemachine.stream.StreamState;
import io.github.lxien.orbien.server.transport.socks5.Socks5ReplyHelper;
import io.github.lxien.orbien.server.transport.connection.DirectConnectionPool;
import io.github.lxien.orbien.server.transport.connection.MultiplexConnectionPool;
import io.github.lxien.orbien.core.transport.TunnelBridge;
import io.github.lxien.orbien.server.transport.bridge.TunnelBridgeFactory;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 流打开成功处理
 */
@Component
public class StreamOpenResponseAction extends StreamBaseAction {
    private final InternalLogger logger = InternalLoggerFactory.getInstance(StreamOpenResponseAction.class);
    @Autowired
    private DirectConnectionPool directConnectionPool;
    @Autowired
    private MultiplexConnectionPool multiplexConnectionPool;
    @Autowired
    private LeastConnHooks leastConnHooks;
    @Autowired
    private MetricsCollector metricsCollector;
    @Autowired
    private InspectorProperties inspectorProperties;
    @Autowired
    private InspectorBuffer inspectorBuffer;

    @Override
    protected void doExecute(StreamState from, StreamState to, StreamEvent event, StreamContext context) {
        logger.debug("收到流 {} 打开响应", context.getStreamId());
        String tunnelId = context.getAndRemoveAs(StreamConstants.TUNNEL_ID, String.class);
        AgentContext agentContext = context.getAgentContext();
        AgentInfo agentInfo = agentContext.getAgentInfo();
        String agentId = agentInfo.getAgentId();
        TunnelEntry tunnelEntry;
        if (context.isMultiplex()) {
            tunnelEntry = multiplexConnectionPool.acquire(agentId, context.getTransportProtocol(), context.isEncrypt());
        } else {
            tunnelEntry = directConnectionPool.borrow(agentId, tunnelId, context.isEncrypt());
        }
        if (tunnelEntry == null) {
            logger.warn("[传输] 连接池无可用隧道 streamId={} agentId={} protocol={} encrypt={} multiplex={}",
                    context.getStreamId(), agentId, context.getTransportProtocol().getName(),
                    context.isEncrypt(), context.isMultiplex());
            context.fireEvent(StreamEvent.STREAM_LOCAL_CLOSE);
            return;
        }
        if (!tunnelEntry.isActive()) {
            logger.warn("[传输] 隧道不可用 streamId={} tunnelId={} protocol={} channelClass={} channelActive={}",
                    context.getStreamId(), tunnelEntry.getTunnelId(),
                    tunnelEntry.getProtocol() != null ? tunnelEntry.getProtocol().getName() : "unknown",
                    tunnelEntry.getChannel().getClass().getSimpleName(),
                    tunnelEntry.getChannel().isActive());
            context.fireEvent(StreamEvent.STREAM_LOCAL_CLOSE);
            return;
        }
        if (!context.isMultiplex() && DirectTunnelLifecycle.isPassthroughActive(tunnelEntry.getChannel())) {
            logger.warn("[传输] 独立隧道仍处于透传模式，无法复用 streamId={} tunnelId={}",
                    context.getStreamId(), tunnelEntry.getTunnelId());
            context.fireEvent(StreamEvent.STREAM_LOCAL_CLOSE);
            return;
        }
        logger.debug("[传输] 流 {} 分配隧道 tunnelId={} protocol={} encrypt={} multiplex={}",
                context.getStreamId(), tunnelEntry.getTunnelId(),
                context.getTransportProtocol().getName(), context.isEncrypt(), context.isMultiplex());
        context.setTunnelEntry(tunnelEntry);
        Channel visitor = context.getVisitor();
        initHttpCaptureIfNeeded(context, visitor);
        TunnelBridge tunnelBridge;
        if (context.isDatagram()) {
            tunnelBridge = TunnelBridgeFactory.buildUdpMux(context);
            logger.debug("UDP 共享隧道建立成功，访问目标: {}", context.getTarget());
        } else if (context.isMultiplex()) {
            tunnelBridge = TunnelBridgeFactory.buildMux(context);
            logger.debug("共享隧道建立成功，访问目标: {}", context.getTarget());
        } else {
            tunnelBridge = TunnelBridgeFactory.buildDirect(context);
            logger.debug("独立隧道建立成功，隧道ID: {}", tunnelEntry.getTunnelId());
        }

        StreamManager streamManager = context.getStreamManager();
        tunnelBridge.openAsync().addListener(future -> {
            if (!future.isSuccess()) {
                logger.error("隧道桥接打开失败 streamId={} multiplex={}",
                        context.getStreamId(), context.isMultiplex(), future.cause());
                context.fireEvent(StreamEvent.STREAM_LOCAL_CLOSE);
                return;
            }
            StreamState state = context.getState();
            if (state == StreamState.CLOSED || state == StreamState.FAILED) {
                logger.debug("流 {} 已在透传切换期间关闭，跳过后续打开步骤", context.getStreamId());
                return;
            }
            context.setTunnelBridge(tunnelBridge);
            streamManager.initStreamIndexes(context);
            leastConnHooks.onStreamOpened(context);
            metricsCollector.onChannelActive(context.getProxyId(), agentInfo.getAgentType());
            if (context.isDatagram()) {
                relayUdpFirstPacket(context, tunnelBridge);
            }
            if (context.isDirectConnection() && !context.isDatagram()) {
                context.markAwaitingClientPassthroughAck();
                notifyDirectPassthroughReady(context);
                logger.debug("流 {} 服务端透传就绪，等待客户端确认后再开启访问者读取", context.getStreamId());
            } else {
                enableVisitorReading(context, visitor, tunnelBridge);
            }
        });
    }

    /**
     * 客户端确认透传就绪后，开启 visitor 读取（及 HTTP 首包转发）
     */
    public void onClientPassthroughReady(StreamContext context) {
        if (!context.compareAndClearAwaitingClientPassthroughAck()) {
            return;
        }
        StreamState state = context.getState();
        if (state == StreamState.CLOSED || state == StreamState.FAILED) {
            logger.debug("流 {} 已关闭，忽略客户端透传确认", context.getStreamId());
            return;
        }
        Channel visitor = context.getVisitor();
        TunnelBridge tunnelBridge = context.getTunnelBridge();
        if (visitor == null || tunnelBridge == null) {
            logger.warn("流 {} 缺少 visitor 或 tunnelBridge，无法完成透传握手", context.getStreamId());
            context.fireEvent(StreamEvent.STREAM_LOCAL_CLOSE);
            return;
        }
        enableVisitorReading(context, visitor, tunnelBridge);
        logger.debug("流 {} 客户端透传就绪，开启访问者读取", context.getStreamId());
    }

    private void enableVisitorReading(StreamContext context, Channel visitor, TunnelBridge tunnelBridge) {
        if (context.getProtocol().isHttp()) {
            relayHttpFirstPackage(context, visitor, tunnelBridge);
            flushPendingUploads(context, tunnelBridge);
        }
        if (context.getProtocol().isSocks5() && context.hasVariable(StreamConstants.SOCKS5_AWAIT_REPLY)) {
            context.removeVariable(StreamConstants.SOCKS5_AWAIT_REPLY);
            Socks5ReplyHelper.sendConnectSuccess(visitor);
        }
        visitor.config().setOption(ChannelOption.AUTO_READ, true);
        visitor.read();
        logger.debug("流 {} 打开成功，可以从访问者读数据", context.getStreamId());
    }

    private void initHttpCaptureIfNeeded(StreamContext context, Channel visitor) {
        if (!InspectorBuffer.shouldCapture(context, inspectorProperties)) {
            logger.debug("[Inspector] 跳过抓包 streamId={} proxyId={} globalEnabled={} inspectorEnabled={}",
                    context.getStreamId(), context.getProxyId(),
                    inspectorProperties != null && inspectorProperties.isEnabled(),
                    context.getProxyConfig() != null && context.getProxyConfig().isInspectorEnabled());
            return;
        }
        HttpStreamCapture capture = new HttpStreamCapture(context, inspectorProperties);
        capture.setCompletionHandler(record -> onHttpCaptureComplete(context, record));
        context.setHttpStreamCapture(capture);
        logger.debug("[Inspector] 开始抓包 streamId={} proxyId={}", context.getStreamId(), context.getProxyId());
        if (visitor != null) {
            ByteBuf firstPacket = visitor.attr(AttributeKeys.HTTP_FIRST_PACKET).get();
            if (firstPacket != null && firstPacket.isReadable()) {
                capture.captureRequestFirstPacket(firstPacket);
            }
        }
    }

    /**
     * HTTP 响应抓包完成：写入缓冲，并在非协议升级场景下关闭当前流。
     * Keep-Alive 连接保留访客 TCP，下一请求会创建新流并重新抓包。
     */
    private void onHttpCaptureComplete(StreamContext context, HttpCaptureRecord record) {
        if (record != null) {
            inspectorBuffer.append(record);
        }
        context.setHttpStreamCapture(null);
        if (record == null || record.getStatus() == 101) {
            return;
        }
        scheduleHttpStreamClose(context);
    }

    private void scheduleHttpStreamClose(StreamContext context) {
        Channel visitor = context.getVisitor();
        if (visitor == null || !visitor.isActive()) {
            return;
        }
        visitor.eventLoop().execute(() -> {
            StreamState state = context.getState();
            if (state == StreamState.OPENED || state == StreamState.PAUSED) {
                logger.debug("[Inspector] HTTP 响应完成，关闭流以支持 Keep-Alive 下一请求 streamId={}",
                        context.getStreamId());
                context.fireEvent(StreamEvent.STREAM_LOCAL_CLOSE);
            }
        });
    }

    /**
     * 发送 HTTP 协议首次缓存的第一个数据包。
     * <p>
     * 使用 {@code getAndSet} 原子取走 attr，避免与 {@link StreamContext#abortLocalForwarding()} 竞态双重释放。
     */
    public void relayHttpFirstPackage(StreamContext context, Channel visitor, TunnelBridge tunnelBridge) {
        ByteBuf cached = visitor.attr(AttributeKeys.HTTP_FIRST_PACKET).getAndSet(null);
        if (cached == null || !cached.isReadable()) {
            logger.debug("[HTTP] 无首个数据包可转发 streamId={} cachedNull={} refCnt={}",
                    context.getStreamId(), cached == null, cached != null ? cached.refCnt() : -1);
            safeRelease(cached);
            return;
        }
        logger.debug("[HTTP] 转发首个数据包 streamId={} bytes={} protocol={} refCnt={}",
                context.getStreamId(), cached.readableBytes(),
                context.getTransportProtocol() != null ? context.getTransportProtocol().getName() : "unknown",
                cached.refCnt());
        // HTTP_FIRST_PACKET 在 HttpVisitorHandler 中已 retain，此处为 sole owner
        tunnelBridge.forwardToLocal(cached, false);
    }

    private static void safeRelease(ByteBuf buf) {
        if (buf != null && buf.refCnt() > 0) {
            ReferenceCountUtil.release(buf);
        }
    }

    /**
     * 流打开前到达的上传 body 分片（OPENING 状态缓存）
     */
    private void flushPendingUploads(StreamContext context, TunnelBridge tunnelBridge) {
        ByteBuf pending;
        while ((pending = context.pollPending()) != null) {
            try {
                if (pending.isReadable()) {
                    logger.debug("[HTTP] 转发打开期间缓存的数据 streamId={} bytes={}",
                            context.getStreamId(), pending.readableBytes());
                    tunnelBridge.forwardToLocal(pending, false);
                } else {
                    safeRelease(pending);
                }
            } catch (RuntimeException ex) {
                safeRelease(pending);
                throw ex;
            }
        }
    }

    private void notifyDirectPassthroughReady(StreamContext context) {
        logger.debug("通知客户端独立隧道透传就绪 streamId={}", context.getStreamId());
        TMSPFrame frame = new TMSPFrame(context.getStreamId(), TMSP.MSG_STREAM_RESUME);
        context.getControl().writeAndFlush(frame);
    }

    public void relayUdpFirstPacket(StreamContext context, TunnelBridge tunnelBridge) {
        ByteBuf cached = context.getPendingFirstPacket();
        context.setPendingFirstPacket(null);
        if (cached == null || cached.refCnt() <= 0 || !cached.isReadable()) {
            safeRelease(cached);
            return;
        }
        logger.debug("转发 UDP 第一个数据包 streamId={}", context.getStreamId());
        tunnelBridge.forwardToLocal(cached, false);
    }
}
