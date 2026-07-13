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

import io.github.lxien.orbien.core.enums.TransportProtocol;
import io.github.lxien.orbien.core.statemachine.context.ProcessContextImpl;
import io.github.lxien.orbien.core.transport.compress.CompressionType;
import io.netty.buffer.ByteBuf;
import io.netty.util.ReferenceCountUtil;
import lombok.Getter;
import lombok.Setter;

import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;


@Getter
@Setter
public abstract class AbstractStreamContext extends ProcessContextImpl {
    protected int streamId;
    protected TunnelEntry tunnelEntry;
    protected boolean compress;
    protected CompressionType compressAlgorithm = CompressionType.NONE;
    protected boolean encrypt;
    protected boolean multiplex;
    protected boolean datagram;
    protected TransportProtocol transportProtocol = TransportProtocol.TCP;
    protected AbstractAgentContext agentContext;
    protected TunnelBridge tunnelBridge;

    /**
     * 流打开前（OPENING）暂存的上传数据
     */
    private final Deque<ByteBuf> pendingQueue = new ConcurrentLinkedDeque<>();

    public void forwardToRemote(ByteBuf payload) {
        forwardToRemote(payload, true);
    }

    public void forwardToRemote(ByteBuf payload, boolean sharedWithInbound) {
        if (tunnelBridge == null) {
            if (!sharedWithInbound) {
                ReferenceCountUtil.release(payload);
            }
            return;
        }
        tunnelBridge.forwardToRemote(payload, sharedWithInbound);
    }

    public void forwardToLocal(ByteBuf payload) {
        forwardToLocal(payload, true);
    }

    public void forwardToLocal(ByteBuf payload, boolean sharedWithInbound) {
        if (tunnelBridge == null) {
            if (!sharedWithInbound) {
                ReferenceCountUtil.release(payload);
            }
            return;
        }
        tunnelBridge.forwardToLocal(payload, sharedWithInbound);
    }

    public boolean isDirectConnection() {
        return !multiplex;
    }

    public CompressionType resolveCompressAlgorithm() {
        if (!compress) {
            return CompressionType.NONE;
        }
        return compressAlgorithm != null && compressAlgorithm.isCompressed()
                ? compressAlgorithm
                : CompressionType.SNAPPY;
    }

    public void enqueue(ByteBuf byteBuf) {
        pendingQueue.offer(byteBuf);
    }

    public ByteBuf pollPending() {
        return pendingQueue.poll();
    }

    public ByteBuf peekPending() {
        return pendingQueue.peek();
    }

    public void enqueueFirst(ByteBuf byteBuf) {
        pendingQueue.offerFirst(byteBuf);
    }

    public void flushPendingToLocal() {
        if (tunnelBridge == null) {
            discardPending();
            return;
        }
        ByteBuf pending;
        while ((pending = pollPending()) != null) {
            try {
                if (pending.isReadable()) {
                    forwardToLocal(pending, true);
                }
            } finally {
                ReferenceCountUtil.release(pending);
            }
        }
    }

    public void discardPending() {
        ByteBuf pending;
        while ((pending = pollPending()) != null) {
            ReferenceCountUtil.release(pending);
        }
    }
}
