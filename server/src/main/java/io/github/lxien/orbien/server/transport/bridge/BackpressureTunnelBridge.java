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

package io.github.lxien.orbien.server.transport.bridge;

import io.github.lxien.orbien.core.transport.TunnelBridge;
import io.github.lxien.orbien.server.statemachine.stream.StreamContext;
import io.netty.buffer.ByteBuf;

public class BackpressureTunnelBridge implements TunnelBridge {
    private final StreamContext streamContext;
    private final TunnelBridge tunnelBridge;

    public BackpressureTunnelBridge(TunnelBridge tunnelBridge, StreamContext streamContext) {
        this.streamContext = streamContext;
        this.tunnelBridge = tunnelBridge;
    }

    @Override
    public void open() {
        tunnelBridge.open();
    }

    @Override
    public void forwardToLocal(ByteBuf payload) {
        tunnelBridge.forwardToLocal(payload);
    }

    @Override
    public void forwardToRemote(ByteBuf payload) {
        tunnelBridge.forwardToRemote(payload);
    }
}
