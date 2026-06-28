/*
 *
 *  *    Copyright 2026 xiaoniucode
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

package com.xiaoniucode.etp.client.manager;

import com.xiaoniucode.etp.client.health.HealthCheckHolder;
import com.xiaoniucode.etp.core.domain.ProxyConfig;
import com.xiaoniucode.etp.core.domain.ProxyConfigExt;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import java.util.*;

public class ProxyManager {
    private final InternalLogger logger = InternalLoggerFactory.getInstance(ProxyManager.class);
    private final Map<String, ProxyConfigExt> map = new ConcurrentHashMap<>();
    private final Set<String> nameSet = ConcurrentHashMap.newKeySet();

    /**
     * 同步来自服务端全量最新配置
     *
     * @param proxies 代理配置
     */
    public void apply(List<ProxyConfigExt> proxies) {
        map.clear();
        nameSet.clear();
        for (ProxyConfigExt config : proxies) {
            ProxyConfig proxyConfig = config.getProxyConfig();

            map.put(proxyConfig.getProxyId(), config);
            nameSet.add(proxyConfig.getName());
            HealthCheckHolder.get().startHealthCheck(proxyConfig);
        }
    }

    public void add(ProxyConfigExt config) {
        ProxyConfig proxyConfig = config.getProxyConfig();
        map.put(proxyConfig.getProxyId(), config);
        nameSet.add(proxyConfig.getName());
        HealthCheckHolder.get().startHealthCheck(proxyConfig);
    }

    public void batchAdd(List<ProxyConfigExt> configs) {
        configs.forEach(this::add);
    }

    public void update(ProxyConfigExt config) {
        if (config == null) {
            return;
        }
        ProxyConfig proxyConfig = config.getProxyConfig();
        delete(proxyConfig.getProxyId());
        add(config);
    }

    public void batchUpdate(List<ProxyConfigExt> configs) {
        if (configs == null || configs.isEmpty()) return;

        for (ProxyConfigExt config : configs) {
            ProxyConfig proxyConfig = config.getProxyConfig();
            delete(proxyConfig.getProxyId());
        }

        for (ProxyConfigExt config : configs) {
            add(config);
        }
    }

    public void delete(String proxyId) {
        ProxyConfigExt removed = map.remove(proxyId);
        if (removed != null) {
            ProxyConfig proxyConfig = removed.getProxyConfig();
            nameSet.remove(proxyConfig.getName());
        }
        HealthCheckHolder.get().stopHealthCheck(proxyId);
    }

    public void batchDelete(List<String> proxyIds) {
        proxyIds.forEach(this::delete);
    }

    public ProxyConfigExt get(String proxyId) {
        return map.get(proxyId);
    }

    public Collection<ProxyConfigExt> list() {
        return map.values();
    }

    public boolean exists(String proxyId) {
        return map.containsKey(proxyId);
    }

    public boolean nameExists(String name) {
        return nameSet.contains(name);
    }

}