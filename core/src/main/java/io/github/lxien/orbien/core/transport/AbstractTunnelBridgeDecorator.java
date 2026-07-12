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

import io.netty.buffer.ByteBuf;
import io.netty.util.concurrent.Future;

public abstract class AbstractTunnelBridgeDecorator implements TunnelBridge {
    protected final TunnelBridge delegate;

    protected AbstractTunnelBridgeDecorator(TunnelBridge delegate) {
        this.delegate = delegate;
    }

    @Override
    public Future<Void> openAsync() {
        return delegate.openAsync();
    }

    @Override
    public void forwardToLocal(ByteBuf payload) {
        forwardToLocal(payload, true);
    }

    @Override
    public void forwardToRemote(ByteBuf payload) {
        forwardToRemote(payload, true);
    }

    @Override
    public void forwardToLocal(ByteBuf payload, boolean sharedWithInbound) {
        delegate.forwardToLocal(payload, sharedWithInbound);
    }

    @Override
    public void forwardToRemote(ByteBuf payload, boolean sharedWithInbound) {
        delegate.forwardToRemote(payload, sharedWithInbound);
    }
}