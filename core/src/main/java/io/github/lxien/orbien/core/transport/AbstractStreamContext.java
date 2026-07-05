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
import io.netty.buffer.ByteBuf;
import lombok.Getter;
import lombok.Setter;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;


@Getter
@Setter
public abstract class AbstractStreamContext extends ProcessContextImpl {
    protected int streamId;
    protected TunnelEntry tunnelEntry;
    protected boolean compress;
    protected boolean encrypt;
    protected boolean multiplex;
    protected boolean datagram;
    protected TransportProtocol transportProtocol = TransportProtocol.TCP;
    protected AbstractAgentContext agentContext;
    protected TunnelBridge tunnelBridge;

    /**
     * 流打开前（OPENING）暂存的上传数据
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
    }

    public ByteBuf pollPending() {
        return pendingQueue.poll();
    }
}
