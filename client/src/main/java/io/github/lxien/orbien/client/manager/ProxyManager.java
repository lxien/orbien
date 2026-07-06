/*
 *
 *  *    Copyright 2026 lxien
 *  *
 *  *    Licensed under the Apache License, Version 2.0 (the "License");
 *  *    you may not use this file except in compliance with the License.
 *  *    You may obtain a copy of the License at
 *  *
 *  *        http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  *    Unless required by applicable law or agreed to in writing, software
 *  *    distributed under the License is distributed on an "AS IS" BASIS,
 *  *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  *    See the License for the specific language governing permissions and
 *  *    limitations under the License.
 *
 */

package io.github.lxien.orbien.client.manager;

import io.github.lxien.orbien.client.health.HealthCheckHolder;
import io.github.lxien.orbien.core.message.Message;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import java.util.*;

public class ProxyManager {
    private final InternalLogger logger = InternalLoggerFactory.getInstance(ProxyManager.class);
    private final Map<String, Message.RuntimeInfo> map = new ConcurrentHashMap<>();

    /**
     * 同步来自服务端全量最新配置
     */
    public void apply(List<Message.RuntimeInfo> proxies) {
        Set<String> incomingIds = new HashSet<>();
        for (Message.RuntimeInfo runtimeInfo : proxies) {
            incomingIds.add(runtimeInfo.getProxyId());
        }
        for (String proxyId : new ArrayList<>(map.keySet())) {
            if (!incomingIds.contains(proxyId)) {
                delete(proxyId);
            }
        }
        map.clear();
        for (Message.RuntimeInfo runtimeInfo : proxies) {
            map.put(runtimeInfo.getProxyId(), runtimeInfo);
            HealthCheckHolder.get().startHealthCheck(runtimeInfo);
        }
    }

    public void add(Message.RuntimeInfo runtimeInfo) {
        map.put(runtimeInfo.getProxyId(), runtimeInfo);
        HealthCheckHolder.get().startHealthCheck(runtimeInfo);
    }

    public void batchAdd(List<Message.RuntimeInfo> configs) {
        configs.forEach(this::add);
    }

    public void batchUpdate(List<Message.RuntimeInfo> configs) {
        if (configs == null || configs.isEmpty()) return;

        for (Message.RuntimeInfo config : configs) {
            delete(config.getProxyId());
        }

        for (Message.RuntimeInfo config : configs) {
            add(config);
        }
    }

    public void delete(String proxyId) {
        map.remove(proxyId);
        HealthCheckHolder.get().stopHealthCheck(proxyId);
    }

    public void batchDelete(List<String> proxyIds) {
        proxyIds.forEach(this::delete);
    }

    public Message.RuntimeInfo get(String proxyId) {
        return map.get(proxyId);
    }

    public Collection<Message.RuntimeInfo> list() {
        return map.values();
    }

}