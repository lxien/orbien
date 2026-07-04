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

package io.github.lxien.orbien.core.transport;

import io.netty.channel.Channel;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 背压流注册表。
 *
 * <p>用于管理因隧道不可写而暂停的流转发记录。当隧道缓冲区满或网络拥塞时，
 * 需要暂停对应隧道上的流数据转发，避免数据丢失。该注册表维护双向映射关系：
 * <ul>
 *   <li>正向映射：{@code Channel -> StreamIds}，记录每个隧道暂停的所有流</li>
 *   <li>反向映射：{@code StreamId -> Channels}，记录每个流被哪些隧道暂停</li>
 * </ul>
 *
 * <p>所有操作均为线程安全，支持并发访问。
 *
 * @author lxien
 */
public class PausedStreamRegistry {

    /**
     * 正向映射：隧道通道到暂停流ID的映射
     * Key: 隧道Channel，Value: 该隧道上暂停的流ID集合
     */
    private final ConcurrentMap<Channel, ConcurrentMap<Integer, Boolean>> pausedStreamByChannel = new ConcurrentHashMap<>();

    /**
     * 反向映射：流ID到隧道通道的映射（用于快速清理）
     * Key: 流ID，Value: 暂停该流的隧道Channel集合
     */
    private final ConcurrentMap<Integer, Set<Channel>> channelsByStreamId = new ConcurrentHashMap<>();

    /**
     * 记录指定隧道上暂停的流ID。
     *
     * @param tunnel   隧道通道
     * @param streamId 流标识符
     */
    public void addPausedStreamId(Channel tunnel, int streamId) {
        pausedStreamByChannel.computeIfAbsent(tunnel, k -> new ConcurrentHashMap<>())
                .putIfAbsent(streamId, Boolean.TRUE);

        channelsByStreamId.computeIfAbsent(streamId,
                        id -> Collections.newSetFromMap(new ConcurrentHashMap<>()))
                .add(tunnel);
    }

    /**
     * 获取指定隧道上所有暂停的流ID集合。
     *
     * @param tunnel 隧道通道
     * @return 暂停的流ID集合，若没有则返回空集合
     */
    public Set<Integer> getPausedStreamIds(Channel tunnel) {
        ConcurrentMap<Integer, Boolean> streams = pausedStreamByChannel.get(tunnel);
        if (streams == null || streams.isEmpty()) {
            return Collections.emptySet();
        }
        return Collections.unmodifiableSet(new HashSet<>(streams.keySet()));
    }

    /**
     * 移除指定隧道上的某个暂停流记录。
     *
     * @param tunnel   隧道通道
     * @param streamId 流标识符
     */
    public void removePausedStream(Channel tunnel, int streamId) {
        ConcurrentMap<Integer, Boolean> streams = pausedStreamByChannel.get(tunnel);
        if (streams != null) {
            streams.remove(streamId);
            if (streams.isEmpty()) {
                pausedStreamByChannel.remove(tunnel, streams);
            }
        }

        Set<Channel> channels = channelsByStreamId.get(streamId);
        if (channels != null) {
            channels.remove(tunnel);
            if (channels.isEmpty()) {
                channelsByStreamId.remove(streamId, channels);
            }
        }
    }

    /**
     * 清理指定流ID的所有暂停记录。
     *
     * @param streamId 流标识符
     */
    public void clear(Integer streamId) {
        Set<Channel> channels = channelsByStreamId.remove(streamId);
        if (channels != null) {
            for (Channel channel : channels) {
                ConcurrentMap<Integer, Boolean> streams = pausedStreamByChannel.get(channel);
                if (streams != null) {
                    streams.remove(streamId);
                    if (streams.isEmpty()) {
                        pausedStreamByChannel.remove(channel, streams);
                    }
                }
            }
        }
    }

    /**
     * 从所有隧道中移除指定的流ID记录。
     *
     * @param streamId 流标识符
     * @see #clear(Integer)
     */
    public void removeStreamIdFromAllTunnels(int streamId) {
        clear(streamId);
    }

    /**
     * 清理指定隧道的所有暂停流记录。
     *
     * @param tunnel 隧道通道
     */
    public void removeByChannel(Channel tunnel) {
        ConcurrentMap<Integer, Boolean> streams = pausedStreamByChannel.remove(tunnel);
        if (streams != null) {
            for (Integer streamId : streams.keySet()) {
                Set<Channel> channels = channelsByStreamId.get(streamId);
                if (channels != null) {
                    channels.remove(tunnel);
                    if (channels.isEmpty()) {
                        channelsByStreamId.remove(streamId, channels);
                    }
                }
            }
        }
    }

    /**
     * 获取暂停了指定流ID的所有隧道通道。
     *
     * @param streamId 流标识符
     * @return 隧道通道集合，若没有则返回空集合
     */
    public Set<Channel> getChannelsByStreamId(int streamId) {
        Set<Channel> channels = channelsByStreamId.get(streamId);
        if (channels == null) {
            return Collections.emptySet();
        }
        return Collections.unmodifiableSet(new HashSet<>(channels));
    }

    /**
     * 判断指定流ID是否被任意隧道暂停。
     *
     * @param streamId 流标识符
     * @return 如果流被暂停返回{@code true}，否则返回{@code false}
     */
    public boolean isStreamPaused(int streamId) {
        return channelsByStreamId.containsKey(streamId);
    }

    /**
     * 获取所有被暂停的流ID集合。
     *
     * @return 暂停流ID的不可变集合
     */
    public Set<Integer> getAllPausedStreamIds() {
        return Collections.unmodifiableSet(new HashSet<>(channelsByStreamId.keySet()));
    }

    /**
     * 清理所有暂停流记录。
     */
    public void clearAll() {
        pausedStreamByChannel.clear();
        channelsByStreamId.clear();
    }

    /**
     * 获取被暂停的流总数。
     *
     * @return 暂停流数量
     */
    public int getPausedStreamCount() {
        return channelsByStreamId.size();
    }

    /**
     * 获取存在暂停流的隧道数量。
     *
     * @return 隧道数量
     */
    public int getChannelCount() {
        return pausedStreamByChannel.size();
    }
}
