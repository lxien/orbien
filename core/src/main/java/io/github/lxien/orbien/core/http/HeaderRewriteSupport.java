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
package io.github.lxien.orbien.core.http;

import io.github.lxien.orbien.core.domain.HeaderRewriteRule;
import io.github.lxien.orbien.core.enums.HeaderAction;
import io.github.lxien.orbien.core.utils.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Header 改写校验、变量渲染与规则应用。
 */
public final class HeaderRewriteSupport {
    public static final int MAX_HEADER_BLOCK = 65536;
    public static final int MAX_RULES = 64;
    public static final int MAX_NAME_LEN = 256;
    public static final int MAX_VALUE_LEN = 1024;

    private static final Pattern TOKEN = Pattern.compile("^[!#$%&'*+.^_`|~0-9A-Za-z-]+$");
    private static final Pattern VAR = Pattern.compile("\\$([a-zA-Z_][a-zA-Z0-9_]*)");
    private static final Set<String> FORBIDDEN = Set.of(
            "content-length", "transfer-encoding", "connection", "keep-alive", "upgrade");
    private static final Set<String> KNOWN_VARS = Set.of("client_ip", "scheme", "host", "request_id");

    private HeaderRewriteSupport() {
    }

    public static void validateRule(HeaderRewriteRule rule) {
        if (rule == null) {
            throw new IllegalArgumentException("规则不能为空");
        }
        if (rule.getAction() == null) {
            throw new IllegalArgumentException("action 不能为空");
        }
        if (!StringUtils.hasText(rule.getName())) {
            throw new IllegalArgumentException("name 不能为空");
        }
        String name = rule.getName().trim();
        if (name.length() > MAX_NAME_LEN || !TOKEN.matcher(name).matches()) {
            throw new IllegalArgumentException("非法的 Header 名称: " + name);
        }
        if (FORBIDDEN.contains(name.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException("禁止改写 Header: " + name);
        }
        rule.setName(name);

        if (rule.getAction() == HeaderAction.REMOVE) {
            rule.setValue(null);
            return;
        }
        if (!StringUtils.hasText(rule.getValue())) {
            throw new IllegalArgumentException("value 不能为空");
        }
        String value = rule.getValue();
        if (value.length() > MAX_VALUE_LEN) {
            throw new IllegalArgumentException("value 过长");
        }
        if (value.indexOf('\r') >= 0 || value.indexOf('\n') >= 0) {
            throw new IllegalArgumentException("value 不能包含换行");
        }
        validateTemplateVars(value);
        rule.setValue(value);
    }

    public static void validateTemplateVars(String value) {
        Matcher matcher = VAR.matcher(value);
        while (matcher.find()) {
            String name = matcher.group(1);
            if (!KNOWN_VARS.contains(name)) {
                throw new IllegalArgumentException("未知变量: $" + name);
            }
        }
    }

    public static String render(String template, Map<String, String> vars) {
        if (template == null || template.isEmpty() || vars == null || vars.isEmpty()) {
            return template;
        }
        Matcher matcher = VAR.matcher(template);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            String key = matcher.group(1);
            String replacement = vars.getOrDefault(key, matcher.group());
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement == null ? "" : replacement));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    public static Map<String, String> buildVars(String clientIp, String scheme, String host) {
        Map<String, String> vars = new LinkedHashMap<>(4);
        vars.put("client_ip", clientIp == null ? "" : clientIp);
        vars.put("scheme", scheme == null ? "http" : scheme);
        vars.put("host", host == null ? "" : host);
        vars.put("request_id", UUID.randomUUID().toString().replace("-", ""));
        return vars;
    }

    /**
     * 应用规则。headers 的 key 为小写，value[0]=写出名称，value[1]=值。
     */
    public static void apply(Map<String, String[]> headers, List<HeaderRewriteRule> rules, Map<String, String> vars) {
        if (headers == null || rules == null || rules.isEmpty()) {
            return;
        }
        for (HeaderRewriteRule rule : rules) {
            if (rule == null || rule.getAction() == null || !StringUtils.hasText(rule.getName())) {
                continue;
            }
            String lower = rule.getName().toLowerCase(Locale.ROOT);
            if (FORBIDDEN.contains(lower)) {
                continue;
            }
            switch (rule.getAction()) {
                case SET -> headers.put(lower, new String[]{rule.getName(), render(rule.getValue(), vars)});
                case ADD -> {
                    if (!headers.containsKey(lower)) {
                        headers.put(lower, new String[]{rule.getName(), render(rule.getValue(), vars)});
                    }
                }
                case REMOVE -> headers.remove(lower);
                default -> {
                }
            }
        }
    }

    public static Map<String, String[]> parseHeaderMap(String[] lines, int start) {
        Map<String, String[]> headers = new LinkedHashMap<>();
        for (int i = start; i < lines.length; i++) {
            String line = lines[i];
            if (line == null || line.isEmpty()) {
                continue;
            }
            int colon = line.indexOf(':');
            if (colon <= 0) {
                continue;
            }
            String name = line.substring(0, colon).trim();
            String value = line.substring(colon + 1).trim();
            headers.put(name.toLowerCase(Locale.ROOT), new String[]{name, value});
        }
        return headers;
    }

    public static String serialize(String startLine, Map<String, String[]> headers) {
        StringBuilder sb = new StringBuilder(256);
        sb.append(startLine).append("\r\n");
        for (String[] entry : headers.values()) {
            sb.append(entry[0]).append(": ").append(entry[1] == null ? "" : entry[1]).append("\r\n");
        }
        sb.append("\r\n");
        return sb.toString();
    }

    public static String headerValue(Map<String, String[]> headers, String name) {
        if (headers == null || name == null) {
            return null;
        }
        String[] entry = headers.get(name.toLowerCase(Locale.ROOT));
        return entry == null ? null : entry[1];
    }
}
