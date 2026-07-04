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

package io.github.lxien.orbien.server.web.service;

import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public final class DomainCertMatcher {

    private DomainCertMatcher() {
    }

    public static List<String> parseSanDomains(String sanDomains) {
        if (!StringUtils.hasText(sanDomains)) {
            return Collections.emptyList();
        }
        return Arrays.stream(sanDomains.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .collect(Collectors.toList());
    }

    public static boolean matches(String domain, List<String> sanDomains) {
        if (!StringUtils.hasText(domain) || sanDomains == null || sanDomains.isEmpty()) {
            return false;
        }
        String normalizedDomain = normalize(domain);
        for (String san : sanDomains) {
            if (!StringUtils.hasText(san)) {
                continue;
            }
            String normalizedSan = normalize(san);
            if (normalizedDomain.equals(normalizedSan)) {
                return true;
            }
            if (normalizedSan.startsWith("*.")) {
                String suffix = normalizedSan.substring(1);
                if (normalizedDomain.endsWith(suffix) && normalizedDomain.length() > suffix.length()) {
                    String prefix = normalizedDomain.substring(0, normalizedDomain.length() - suffix.length());
                    if (!prefix.contains(".")) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static String normalize(String domain) {
        String value = domain.trim().toLowerCase(Locale.ROOT);
        if (value.endsWith(".")) {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }
}
