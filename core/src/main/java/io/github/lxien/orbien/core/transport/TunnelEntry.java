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

import io.github.lxien.orbien.core.enums.TunnelType;
import io.netty.channel.Channel;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TunnelEntry {
    private String tunnelId;
    private boolean active;
    private TunnelType tunnelType;
    private boolean encrypt;
    private Channel channel;
    private NettyBatchWriteQueue writeQueue;

    public TunnelEntry(String tunnelId, boolean encrypt, Channel channel, TunnelType tunnelType, NettyBatchWriteQueue writeQueue) {
        this.tunnelId = tunnelId;
        this.channel = channel;
        this.encrypt = encrypt;
        this.tunnelType = tunnelType;
        this.writeQueue = writeQueue;
    }

    public TunnelEntry(String tunnelId, boolean encrypt, Channel channel, TunnelType tunnelType) {
        this(tunnelId, encrypt, channel, tunnelType, null);
    }

    public boolean isActive() {
        return channel != null && channel.isActive();
    }
}
