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

public interface TunnelBridge {

    /**
     * 异步打开桥接。独立隧道在隧道 EventLoop 上切换 pipeline 后完成；多路复用立即完成。
     */
    Future<Void> openAsync();

    void forwardToLocal(ByteBuf payload);

    void forwardToRemote(ByteBuf payload);
}