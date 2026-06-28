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

package com.xiaoniucode.etp.server.vhost;

import com.xiaoniucode.etp.core.enums.DomainType;
import com.xiaoniucode.etp.server.exceptions.DomainConflictException;
import com.xiaoniucode.etp.server.service.ProxyConfigService;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class DomainGenerator {
    private final InternalLogger logger = InternalLoggerFactory.getInstance(DomainGenerator.class);
    private static final int MIN_PREFIX_LENGTH = 1;
    private static final int MAX_PREFIX_LENGTH = 10;
    private static final String PREFIX_CHARS = "abcdefghijklmnopqrstuvwxyz0123456789";
    @Autowired
    private ProxyConfigService proxyConfigService;

    public com.xiaoniucode.etp.core.domain.DomainInfo generateRandomSubdomain(String baseDomain) throws DomainConflictException {
        return generateRandomDomainPrefix(baseDomain);
    }

    private com.xiaoniucode.etp.core.domain.DomainInfo generateRandomDomainPrefix(String baseDomain) {
        for (int i = 0; i < 20; i++) {
            String prefix = generateRandomPrefix();
            if (!proxyConfigService.exists(prefix + "." + baseDomain)) {
                return new com.xiaoniucode.etp.core.domain.DomainInfo(baseDomain, prefix, DomainType.AUTO);

            }
        }
        return null;
    }

    private String generateRandomPrefix() {
        int length = ThreadLocalRandom.current().nextInt(MIN_PREFIX_LENGTH, MAX_PREFIX_LENGTH + 1);
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(PREFIX_CHARS.charAt(ThreadLocalRandom.current().nextInt(PREFIX_CHARS.length())));
        }
        return sb.toString();
    }

    public Set<com.xiaoniucode.etp.core.domain.DomainInfo> generateSubdomains(String baseDomain, Set<String> subDomains) {
        Set<com.xiaoniucode.etp.core.domain.DomainInfo> res = new HashSet<>();
        //todo 一次性检查所有子域名是否存在，避免多次查询
        for (String subDomain : subDomains) {
            if (proxyConfigService.exists(subDomain + "." + baseDomain)) {
                throw new DomainConflictException("域名[" + subDomain + "." + baseDomain + "]已被占用");
            }
            res.add(new com.xiaoniucode.etp.core.domain.DomainInfo(baseDomain, subDomain, DomainType.SUBDOMAIN));
        }
        return res;
    }

    public Set<com.xiaoniucode.etp.core.domain.DomainInfo> generateCustomDomains(Set<String> customDomains) {
        //todo 一次性检查所有子域名是否存在，避免多次查询
        Set<com.xiaoniucode.etp.core.domain.DomainInfo> domainInfos = new HashSet<>();
        for (String domain : customDomains) {
            if (proxyConfigService.exists(domain)) {
                throw new DomainConflictException("域名[" + domain + "]已被占用");
            }
            domainInfos.add(new com.xiaoniucode.etp.core.domain.DomainInfo(null, domain, DomainType.CUSTOM_DOMAIN));
        }
        return domainInfos;
    }
}
