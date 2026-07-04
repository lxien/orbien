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

package io.github.lxien.orbien.server.vhost;

import io.github.lxien.orbien.server.exceptions.OrbienException;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class DomainRegistry {
    private final Map<String/*proxyId*/, Set<String/*domain*/>> proxyDomains = new ConcurrentHashMap<>();
    private final Map<String/*domain*/, String/*proxyId*/> domainIndex = new ConcurrentHashMap<>();

    public boolean exists(String proxyId) {
        return proxyDomains.containsKey(proxyId);
    }
    public void register(String proxyId, Set<String> domains) {
        for (String domain : domains) {
            String exist = domainIndex.get(domain);
            if (exist != null && !exist.equals(proxyId)) {
                throw new OrbienException("域名冲突: " + domain);
            }
        }
        proxyDomains.put(proxyId, domains);
        for (String domain : domains) {
            domainIndex.put(domain, proxyId);
        }
    }

    public void unregister(String proxyId) {
        Set<String> domains = proxyDomains.remove(proxyId);
        if (domains != null) {
            for (String domain : domains) {
                domainIndex.remove(domain);
            }
        }
    }

    public Set<String> getDomainsByProxyId(String proxyId) {
        return proxyDomains.getOrDefault(proxyId, new HashSet<>());
    }

    public String getProxyIdByDomain(String domain) {
        return domainIndex.get(domain);
    }
}