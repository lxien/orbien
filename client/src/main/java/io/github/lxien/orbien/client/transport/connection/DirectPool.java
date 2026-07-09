package io.github.lxien.orbien.client.transport.connection;

import io.github.lxien.orbien.client.util.UUIDGenerator;
import io.github.lxien.orbien.core.enums.TunnelType;
import io.github.lxien.orbien.core.transport.TunnelEntry;
import io.github.lxien.orbien.core.utils.ChannelUtils;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 独立隧道连接池
 * 管理明文和加密隧道的连接池，支持连接的创建、借用、释放和移除
 */
@Getter
@Setter
public class DirectPool {
    /**
     * 日志记录器
     */
    private final InternalLogger logger = InternalLoggerFactory.getInstance(DirectPool.class);

    /**
     * 最大隧道池大小
     */
    private static final int MAX_TUNNEL_POOL_SIZE = 100;

    /**
     * 明文隧道映射
     */
    private final Map<String, TunnelEntry> plainTunnels = new ConcurrentHashMap<>(5);

    /**
     * 加密隧道映射
     */
    private final Map<String, TunnelEntry> encryptTunnels = new ConcurrentHashMap<>(5);

    /**
     * 借用指定类型的隧道
     *
     * @param isEncrypt 是否为加密隧道
     * @return 可用的隧道入口，如果没有可用隧道则返回null
     */
    public TunnelEntry borrow(boolean isEncrypt) {
        Map<String, TunnelEntry> tunnels = isEncrypt ? encryptTunnels : plainTunnels;
        for (Map.Entry<String, TunnelEntry> mapEntry : tunnels.entrySet()) {
            TunnelEntry entry = mapEntry.getValue();
            if (entry.isActive()) {
                return tunnels.remove(mapEntry.getKey());
            } else {
                logger.debug("隧道 {} 不活跃，从池中移除", entry.getTunnelId());
                removeTunnel(entry.getTunnelId());
            }
        }
        logger.debug("池中没有可用的活跃{}隧道", isEncrypt ? "加密" : "明文");
        return null;
    }

    /**
     * 创建指定类型的隧道
     *
     * @param channel   通道
     * @param isEncrypt 是否为加密隧道
     * @return 隧道入口，如果池已满则返回null
     */
    public TunnelEntry createTunnel(Channel channel, boolean isEncrypt) {
        if (plainTunnels.size() + encryptTunnels.size() >= MAX_TUNNEL_POOL_SIZE) {
            return null;
        }
        String tunnelId = UUIDGenerator.generate();
        //NettyBatchWriteQueue writeQueue = NettyBatchWriteQueue.createWriteQueue(channel);
        TunnelEntry tunnelEntry = new TunnelEntry(tunnelId, isEncrypt, channel, TunnelType.DIRECT);
        if (isEncrypt) {
            encryptTunnels.putIfAbsent(tunnelId, tunnelEntry);
        } else {
            plainTunnels.putIfAbsent(tunnelId, tunnelEntry);
        }
        return tunnelEntry;
    }

    /**
     * 激活隧道
     *
     * @param tunnelId 隧道ID
     * @return 激活后的隧道入口，如果隧道不存在则返回null
     */
    public TunnelEntry activateTunnel(String tunnelId) {
        TunnelEntry entry = plainTunnels.get(tunnelId);
        if (entry == null) {
            entry = encryptTunnels.get(tunnelId);
        }
        if (entry != null) {
            entry.setActive(true);
            return entry;
        }
        return null;
    }

    /**
     * 释放隧道
     *
     * @param entry 隧道入口
     */
    public void release(TunnelEntry entry) {
        if (entry == null) {
            return;
        }
        Channel tunnel = entry.getChannel();
        int totalSize = plainTunnels.size() + encryptTunnels.size();
        if (totalSize > MAX_TUNNEL_POOL_SIZE) {
            tunnel.close();
            removeTunnel(entry.getTunnelId());
        } else {
            tunnel.config().setOption(ChannelOption.AUTO_READ, true);
            if (entry.isEncrypt()) {
                encryptTunnels.putIfAbsent(entry.getTunnelId(), entry);
            } else {
                plainTunnels.putIfAbsent(entry.getTunnelId(), entry);
            }
        }
    }

    /**
     * 移除隧道
     *
     * @param tunnelId 隧道ID
     */
    public void removeTunnel(String tunnelId) {
        TunnelEntry entry = plainTunnels.remove(tunnelId);
        if (entry == null) {
            entry = encryptTunnels.remove(tunnelId);
        }
        if (entry != null) {
            Channel tunnel = entry.getChannel();
            if (tunnel != null && tunnel.isActive()) {
                tunnel.close();
            }
        }
    }

    /**
     * 获取明文隧道数量
     *
     * @return 明文隧道数量
     */
    public int getPlainTunnelCount() {
        return plainTunnels.size();
    }

    /**
     * 获取加密隧道数量
     *
     * @return 加密隧道数量
     */
    public int getEncryptTunnelCount() {
        return encryptTunnels.size();
    }

    /**
     * 获取总隧道数量
     *
     * @return 总隧道数量
     */
    public int getTotalTunnelCount() {
        return plainTunnels.size() + encryptTunnels.size();
    }

    public void closeAll() {
        plainTunnels.values().forEach(tunnelEntry ->
                ChannelUtils.closeOnFlush(tunnelEntry.getChannel()));
        encryptTunnels.values().forEach(tunnelEntry ->
                ChannelUtils.closeOnFlush(tunnelEntry.getChannel()));
    }
}
