/*
 *    Copyright 2026 xiaoniucode
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
package com.xiaoniucode.etp.server.security;

import com.xiaoniucode.etp.core.domain.AccessControlConfig;
import com.xiaoniucode.etp.core.domain.ProxyConfig;
import com.xiaoniucode.etp.core.ip.cidr.CIDRMatcher;
import com.xiaoniucode.etp.core.enums.AccessControl;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class IpAccessChecker {
    private final Map<String/*proxyId*/, CIDRMatcher> matcherMap = new ConcurrentHashMap<>();

    public boolean checkAccess(String proxyId, AccessControlConfig accessControl, String visitorIp) {
        if (proxyId == null || visitorIp == null) {
            return false;
        }
        if (accessControl == null || !accessControl.isEnabled()) {
            matcherMap.remove(proxyId);
            return true;
        }

        AccessControl mode = accessControl.getMode();
        Set<String> allow = accessControl.getAllow();
        if (mode.isAllowMode() && allow.isEmpty()) {
            //白名单模式 + 空列表 = 拒绝所有
            return false;
        }
        Set<String> deny = accessControl.getDeny();
        if (mode.isDenyMode() && deny.isEmpty()) {
            //黑名单模式 + 空列表 = 允许所有
            return true;
        }

        CIDRMatcher matcher = matcherMap.computeIfAbsent(proxyId, s -> new CIDRMatcher(mode, allow, deny));
        return matcher.isAllowed(visitorIp);
    }

    public void invalidate(String proxyId) {
        matcherMap.remove(proxyId);
    }

    public void invalidateAll() {
        matcherMap.clear();
    }
}
