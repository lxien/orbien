package io.github.lxien.orbien.server.statemachine.stream;

import com.alibaba.cola.statemachine.StateMachine;
import io.github.lxien.orbien.common.utils.StringUtils;
import io.github.lxien.orbien.core.domain.BandwidthConfig;
import io.github.lxien.orbien.core.enums.ProtocolType;
import io.github.lxien.orbien.core.transport.AttributeKeys;
import io.github.lxien.orbien.core.transport.PausedStreamRegistry;
import io.github.lxien.orbien.core.transport.TunnelEntry;
import io.github.lxien.orbien.core.transport.UdpSessionKey;
import io.github.lxien.orbien.server.generator.StreamIdGenerator;
import io.github.lxien.orbien.server.transport.BandwidthLimiter;
import io.github.lxien.orbien.core.transport.VisitorAddressResolver;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class StreamManager {
    private final InternalLogger logger = InternalLoggerFactory.getInstance(StreamManager.class);
    /**
     * streamId -->连接上下文
     */
    private final Map<Integer, StreamContext> visitors = new ConcurrentHashMap<>();
    private final PausedStreamRegistry pausedStreamRegistry = new PausedStreamRegistry();
    /**
     * proxyId --> BandWithLimiter
     */
    private final Map<String, BandwidthLimiter> proxyLimiters = new ConcurrentHashMap<>();
    /**
     * proxyId ->
     * 存储每个代理的活跃流计数
     */
    private final Map<String, AtomicInteger> proxyStreamCount = new ConcurrentHashMap<>();

    /**
     * remotePort -> Set<streamId>
     * 用于快速按端口批量关闭流
     */
    private final Map<Integer, Set<Integer>> portToStreams = new ConcurrentHashMap<>();

    /**
     * domain -> Set<streamId>
     * 用于快速按域名批量关闭流
     */
    private final Map<String, Set<Integer>> domainToStreams = new ConcurrentHashMap<>();

    /**
     * agentId -> Set<streamId>
     * 用于快速按客户端清理所有流
     */
    private final Map<String, Set<Integer>> agentToStreams = new ConcurrentHashMap<>();

    /**
     * UDP 会话索引
     */
    private final Map<UdpSessionKey, Integer> udpSessionIndex = new ConcurrentHashMap<>();

    /**
     * UDP 会话 idle 超时任务
     */
    private final Map<UdpSessionKey, AtomicReference<io.netty.util.concurrent.ScheduledFuture<?>>> udpSessionTimeouts = new ConcurrentHashMap<>();

    /**
     * remotePort -> Set<streamId> (UDP)
     */
    private final Map<Integer, Set<Integer>> udpPortToStreams = new ConcurrentHashMap<>();

    @Autowired
    private StreamIdGenerator streamIdGenerator;

    /**
     * 创建并保存新的流上下文
     */
    public StreamContext createStreamContext(Channel visitor, StateMachine<StreamState, StreamEvent, StreamContext> stateMachine) {
        int streamId = streamIdGenerator.nextStreamId();
        StreamContext streamContext = new StreamContext(streamId, stateMachine);
        streamContext.setVisitor(visitor);
        streamContext.setSourceAddress(getRemoteAddress(visitor));
        visitor.attr(AttributeKeys.STREAM_ID).set(streamId);
        visitors.put(streamId, streamContext);
        return streamContext;
    }

    public StreamContext createUdpStreamContext(Channel datagramChannel,
                                                java.net.InetSocketAddress sender,
                                                StateMachine<StreamState, StreamEvent, StreamContext> stateMachine) {
        int streamId = streamIdGenerator.nextStreamId();
        StreamContext streamContext = new StreamContext(streamId, stateMachine);
        streamContext.setVisitor(datagramChannel);
        streamContext.setVisitorAddress(sender);
        streamContext.setSourceAddress(sender.toString());
        visitors.put(streamId, streamContext);
        return streamContext;
    }

    public void registerUdpSession(UdpSessionKey sessionKey, int streamId) {
        udpSessionIndex.put(sessionKey, streamId);
    }

    public void unregisterUdpSession(UdpSessionKey sessionKey) {
        udpSessionIndex.remove(sessionKey);
        AtomicReference<io.netty.util.concurrent.ScheduledFuture<?>> timeoutRef = udpSessionTimeouts.remove(sessionKey);
        if (timeoutRef != null && timeoutRef.get() != null) {
            timeoutRef.get().cancel(false);
        }
    }

    public Optional<StreamContext> getStreamContextByUdpSession(UdpSessionKey sessionKey) {
        Integer streamId = udpSessionIndex.get(sessionKey);
        if (streamId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(visitors.get(streamId));
    }

    public void scheduleUdpSessionTimeout(UdpSessionKey sessionKey,
                                          io.netty.channel.EventLoop eventLoop,
                                          long timeoutSeconds) {
        AtomicReference<io.netty.util.concurrent.ScheduledFuture<?>> timeoutRef =
                udpSessionTimeouts.computeIfAbsent(sessionKey, key -> new AtomicReference<>());
        io.netty.util.concurrent.ScheduledFuture<?> existing = timeoutRef.get();
        if (existing != null) {
            existing.cancel(false);
        }
        timeoutRef.set(eventLoop.schedule(() -> {
            Integer streamId = udpSessionIndex.get(sessionKey);
            if (streamId != null) {
                StreamContext ctx = visitors.get(streamId);
                if (ctx != null) {
                    logger.debug("UDP 会话 idle 超时，关闭流 streamId={}", streamId);
                    ctx.fireEvent(StreamEvent.STREAM_LOCAL_CLOSE);
                }
            }
        }, timeoutSeconds, TimeUnit.SECONDS));
    }

    public void touchUdpSession(UdpSessionKey sessionKey) {
        StreamContext ctx = getStreamContextByUdpSession(sessionKey).orElse(null);
        if (ctx != null && ctx.getVisitor() != null) {
            scheduleUdpSessionTimeout(sessionKey, ctx.getVisitor().eventLoop(), 60);
        }
    }

    public void removeStreamContext(int streamId) {
        StreamContext streamContext = visitors.remove(streamId);
        if (!Objects.isNull(streamContext)) {
            String proxyId = streamContext.getProxyId();
            //关闭代理连接计数
            if (StringUtils.hasText(proxyId)) {
                decrementStreamCount(proxyId);
            }
            //清理所有索引映射
            cleanupIndexes(streamContext);
        } else {
            logger.warn("删除流上下文失败，流 {} 不存在 ", streamId);
        }

    }

    /**
     * 清理所有索引映射（与 initStreamRelations 对应）
     */
    private void cleanupIndexes(StreamContext context) {
        if (context == null) {
            return;
        }

        int streamId = context.getStreamId();
        ProtocolType protocol = context.getProtocol();
        // 清理端口索引
        if (protocol.isTcp()) {
            Integer listenerPort = context.getListenerPort();
            if (listenerPort != null) {
                Set<Integer> portSet = portToStreams.get(listenerPort);
                if (portSet != null) {
                    portSet.remove(streamId);
                    if (portSet.isEmpty()) {
                        portToStreams.remove(listenerPort);
                    }
                }
            }
        }
        if (protocol.isUdp()) {
            Integer listenerPort = context.getListenerPort();
            if (listenerPort != null) {
                Set<Integer> portSet = udpPortToStreams.get(listenerPort);
                if (portSet != null) {
                    portSet.remove(streamId);
                    if (portSet.isEmpty()) {
                        udpPortToStreams.remove(listenerPort);
                    }
                }
            }
            if (context.getVisitorAddress() != null && listenerPort != null) {
                unregisterUdpSession(UdpSessionKey.of(listenerPort, context.getVisitorAddress()));
            }
            ByteBuf pending = context.getPendingFirstPacket();
            if (pending != null && pending.refCnt() > 0) {
                pending.release();
                context.setPendingFirstPacket(null);
            }
        }

        // 清理域名索引
        if (protocol.isHttp()) {
            String domain = context.getVisitorDomain();
            if (StringUtils.hasText(domain)) {
                String normalizedDomain = domain.trim().toLowerCase(Locale.ROOT);
                Set<Integer> domainSet = domainToStreams.get(normalizedDomain);
                if (domainSet != null) {
                    domainSet.remove(streamId);
                    if (domainSet.isEmpty()) {
                        domainToStreams.remove(normalizedDomain);
                    }
                }
            }
        }

        // 清理 Agent 索引
        if (context.hasAgent()) {
            String agentId = context.getAgentId();
            Set<Integer> agentSet = agentToStreams.get(agentId);
            if (agentSet != null) {
                agentSet.remove(streamId);
                if (agentSet.isEmpty()) {
                    agentToStreams.remove(agentId);
                }
            }
        }
    }

    public Optional<StreamContext> getStreamContext(Channel visitor) {
        Integer streamId = visitor.attr(AttributeKeys.STREAM_ID).get();
        if (streamId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(visitors.get(streamId));
    }

    /**
     * 初始化索引，在流完全打开成功以后调用
     *
     * @param context 流上下文
     */
    public void initStreamIndexes(StreamContext context) {
        if (context == null) {
            return;
        }
        int streamId = context.getStreamId();
        ProtocolType protocol = context.getProtocol();
        if (protocol.isHttp()) {
            //处理域名映射 仅限HTTP 代理
            String domain = context.getVisitorDomain();
            if (StringUtils.hasText(domain)) {
                String normalizedDomain = domain.trim().toLowerCase(Locale.ROOT);
                domainToStreams.computeIfAbsent(normalizedDomain,
                        k -> ConcurrentHashMap.newKeySet()).add(streamId);
            }
        }
        if (protocol.isTcp()) {
            //处理端口映射 仅限TCP 代理
            Integer listenerPort = context.getListenerPort();
            if (listenerPort != null) {
                portToStreams.computeIfAbsent(listenerPort,
                        k -> ConcurrentHashMap.newKeySet()).add(streamId);
            }
        }
        if (protocol.isUdp()) {
            Integer listenerPort = context.getListenerPort();
            if (listenerPort != null) {
                udpPortToStreams.computeIfAbsent(listenerPort,
                        k -> ConcurrentHashMap.newKeySet()).add(streamId);
            }
        }
        //处理客户端映射
        String agentId = context.getAgentId();
        if (StringUtils.hasText(agentId)) {
            agentToStreams.computeIfAbsent(agentId,
                    k -> ConcurrentHashMap.newKeySet()).add(streamId);
        }
        if (logger.isDebugEnabled()) {
            logger.debug("流索引映射初始化完成 streamId={}, agentId={}", streamId, agentId);
        }
    }

    public Set<Integer> getStreamIdsByPort(Integer remotePort) {
        return portToStreams.get(remotePort);
    }


    public Set<Integer> getStreamIdsByDomain(String domain) {
        return domainToStreams.get(domain);
    }


    public Set<Integer> getStreamIdsByAgent(String agentId) {
        return agentToStreams.get(agentId);
    }

    /**
     * 按端口触发关闭
     */
    public void fireCloseByPort(Integer remotePort) {
        fireCloseBy(portToStreams.get(remotePort));
    }

    public void fireCloseByUdpPort(Integer remotePort) {
        fireCloseBy(udpPortToStreams.get(remotePort));
    }

    /**
     * 按域名触发关闭
     */
    public void fireCloseByDomain(String domain) {
        fireCloseBy(domainToStreams.get(domain));
    }

    /**
     * 按 Agent 触发关闭
     */
    public void fireCloseByAgent(String agentId) {
        fireCloseBy(agentToStreams.get(agentId));
    }

    private void fireCloseBy(Set<Integer> streamIds) {
        if (streamIds == null || streamIds.isEmpty()) {
            return;
        }

        for (Integer streamId : new ArrayList<>(streamIds)) {
            if (streamId == null) continue;
            StreamContext ctx = getStreamContext(streamId);
            if (ctx != null) {
                ctx.fireEvent(StreamEvent.STREAM_LOCAL_CLOSE);
            }
        }
    }

    public void closeStreamsByTunnel(Channel tunnel) {
        if (tunnel == null) {
            return;
        }
        for (StreamContext ctx : new ArrayList<>(visitors.values())) {
            if (ctx == null) {
                continue;
            }
            TunnelEntry entry = ctx.getTunnelEntry();
            if (entry != null && entry.getChannel() == tunnel) {
                ctx.abortLocalForwarding();
                StreamState state = ctx.getState();
                if (state != StreamState.CLOSED && state != StreamState.FAILED) {
                    ctx.fireEvent(StreamEvent.STREAM_LOCAL_CLOSE);
                }
            }
        }
        pausedStreamRegistry.removeByChannel(tunnel);
    }

    public void addPausedStreamId(Channel tunnel, int streamId) {
        pausedStreamRegistry.addPausedStreamId(tunnel, streamId);
    }

    public   Set<Integer>  getPausedStreamIds(Channel tunnel) {
        return pausedStreamRegistry.getPausedStreamIds(tunnel);
    }

    public void removePausedStream(Channel tunnel, int streamId) {
        pausedStreamRegistry.removePausedStream(tunnel, streamId);
    }

    /**
     * 每一个代理 一个限流器
     *
     * @param proxyId         代理ID
     * @param bandwidthConfig 限流配置
     * @return 限流器
     */
    public BandwidthLimiter getOrCreateProxyLimiter(String proxyId, BandwidthConfig bandwidthConfig) {
        return proxyLimiters.computeIfAbsent(proxyId, id -> {
            proxyStreamCount.put(proxyId, new AtomicInteger(0));
            logger.debug("创建代理限流器：proxyId={}", proxyId);
            return new BandwidthLimiter(bandwidthConfig);
        });
    }

    public void incrementStreamCount(String proxyId) {
        AtomicInteger count = proxyStreamCount.get(proxyId);
        if (count != null) {
            count.incrementAndGet();
        }
    }

    private void decrementStreamCount(String proxyId) {
        AtomicInteger count = proxyStreamCount.get(proxyId);
        if (count != null && count.decrementAndGet() == 0) {
            // 最后一个流关闭，释放限流器
            proxyLimiters.remove(proxyId);
            proxyStreamCount.remove(proxyId);
            logger.debug("释放代理限流器：proxyId={}", proxyId);
        }
    }

    /**
     * 获取客户端远程地址
     */
    private String getRemoteAddress(Channel visitor) {
        try {
            String ip = VisitorAddressResolver.resolveIp(visitor);
            return ip != null ? ip : "unknown";
        } catch (Exception e) {
            return "unknown";
        }
    }

    public StreamContext getStreamContext(Integer streamId) {
        return visitors.get(streamId);
    }

    /**
     * 判断是否存在
     */
    public boolean containsStream(Integer streamId) {
        return visitors.containsKey(streamId);
    }

    /**
     * 获取所有流数量
     */
    public int getStreamCount() {
        return visitors.size();
    }


}
