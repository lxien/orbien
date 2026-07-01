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

import com.xiaoniucode.etp.core.domain.DomainInfo;
import com.xiaoniucode.etp.core.enums.DomainType;
import com.xiaoniucode.etp.server.exceptions.DomainConflictException;
import com.xiaoniucode.etp.server.service.DomainConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class DomainGenerator {
    private static final int PREFIX_LENGTH = 6;
    private static final String PREFIX_CHARS = "abcdefghijklmnopqrstuvwxyz";
    @Autowired
    private DomainConfigService domainConfigService;

    public DomainInfo generateRandomSubdomain(String baseDomain) throws DomainConflictException {
        return generateRandomDomainPrefix(baseDomain);
    }

    private DomainInfo generateRandomDomainPrefix(String baseDomain) {
        int maxRetries = 50;
        for (int i = 0; i < maxRetries; i++) {
            String prefix = generateRandomPrefix();
            if (!domainConfigService.exists(prefix + "." + baseDomain)) {
                return new DomainInfo(baseDomain, prefix, DomainType.AUTO);
            }
        }
        String prefix = generateRandomPrefix(8);
        return new DomainInfo(baseDomain, prefix, DomainType.AUTO);
    }

    private String generateRandomPrefix() {
        return generateRandomPrefix(PREFIX_LENGTH);
    }

    private String generateRandomPrefix(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(PREFIX_CHARS.charAt(ThreadLocalRandom.current().nextInt(PREFIX_CHARS.length())));
        }
        return sb.toString();
    }

    public List<DomainInfo> generateSubdomains(String baseDomain, List<String> subDomains) {
        List<DomainInfo> res = new ArrayList<>();
        for (String subDomain : subDomains) {
            if (domainConfigService.exists(subDomain + "." + baseDomain)) {
                throw new DomainConflictException("域名[" + subDomain + "." + baseDomain + "]已被占用");
            }
            res.add(new DomainInfo(baseDomain, subDomain, DomainType.SUBDOMAIN));
        }
        return res;
    }

    public List<DomainInfo> generateCustomDomains(List<String> customDomains) {
        List<DomainInfo> domainInfos = new ArrayList<>();
        for (String domain : customDomains) {
            if (domainConfigService.exists(domain)) {
                throw new DomainConflictException("域名[" + domain + "]已被占用");
            }
            domainInfos.add(new DomainInfo(null, domain, DomainType.CUSTOM_DOMAIN));
        }
        return domainInfos;
    }
}
