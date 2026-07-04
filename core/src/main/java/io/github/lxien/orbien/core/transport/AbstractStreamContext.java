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

import io.github.lxien.orbien.core.statemachine.context.ProcessContextImpl;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import lombok.Getter;
import lombok.Setter;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;


@Getter
@Setter
public abstract class AbstractStreamContext extends ProcessContextImpl {
    protected int streamId;
    protected TunnelEntry tunnelEntry;
    protected boolean compress;
    protected boolean encrypt;
    protected boolean multiplex;
    protected boolean datagram;
    protected AbstractAgentContext agentContext;
    protected TunnelBridge tunnelBridge;

    /**
     * 高水位，超过此水位通知远端暂停发送数据
     */
    protected final long DEFAULT_HIGH_WATERMARK = 8 * 1024 * 1024;

    /**
     * 低水位，低于此值通知远端恢复数据发送
     */
    protected final long DEFAULT_LOW_WATERMARK = 2 * 1024 * 1024;

    private long highWaterMark = DEFAULT_HIGH_WATERMARK;
    private long lowWaterMark = DEFAULT_LOW_WATERMARK;
    /**
     * 当前待发送总字节数
     */
    private final AtomicLong pendingBytes = new AtomicLong();
    /**
     * 待发送缓冲队列
     */
    private final Queue<ByteBuf> pendingQueue = new ConcurrentLinkedQueue<>();

    public void forwardToRemote(ByteBuf payload) {
        if (tunnelBridge == null) {
            return;
        }
        tunnelBridge.forwardToRemote(payload);
    }

    public void forwardToLocal(ByteBuf payload) {
        if (tunnelBridge == null) {
            return;
        }
        tunnelBridge.forwardToLocal(payload);
    }

    public boolean isDirectConnection() {
        return !multiplex;
    }

    public boolean isDatagram() {
        return datagram;
    }

    public void enqueue(ByteBuf byteBuf) {
        pendingQueue.offer(byteBuf);
        pendingBytes.addAndGet(byteBuf.readableBytes());
    }

    public ByteBuf pollPending() {
        ByteBuf buf = pendingQueue.poll();
        if (buf != null) {
            pendingBytes.addAndGet(-buf.readableBytes());
        }
        return buf;
    }

    public boolean isHighWatermark() {
        return pendingBytes.get() >= getHighWaterMark();
    }

    public boolean isLowWatermark() {
        return pendingBytes.get() <= getLowWaterMark();
    }
}
