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
package io.github.lxien.orbien.core.ip.cidr;


import io.github.lxien.orbien.core.enums.AccessControl;
import lombok.Getter;

import java.util.*;

/**
 * 基于区间 + 二分查找的 CIDR 匹配器
 */
public class CIDRMatcher {

    @Getter
    private final List<CIDRRange> ranges;
    private final boolean allowMode;

    public CIDRMatcher(AccessControl mode, Set<String> allowList, Set<String> denyList) {
        this.allowMode = AccessControl.ALLOW.equals(mode);

        List<CIDRRange> temp = new ArrayList<>();
        Set<String> source = allowMode ? allowList : denyList;

        if (source != null && !source.isEmpty()) {
            for (String cidr : source) {
                if (cidr == null || cidr.isEmpty()) {
                    continue;
                }
                try {
                    parseCIDR(cidr, temp);
                } catch (Exception ignore) {
                }
            }

            if (!temp.isEmpty()) {
                temp.sort(Comparator.comparingLong(CIDRRange::getStartIp));
                temp = merge(temp);
            }
        }

        this.ranges = Collections.unmodifiableList(temp);
    }

    /**
     * CIDR 转区间
     */
    private void parseCIDR(String cidr, List<CIDRRange> out) {
        int slash = cidr.indexOf('/');

        String ipPart;
        int prefix;

        if (slash > 0) {
            ipPart = cidr.substring(0, slash);
            prefix = parseIntSafe(cidr, slash + 1);
        } else {
            ipPart = cidr;
            prefix = 32;
        }

        if (prefix < 0 || prefix > 32) {
            return;
        }

        String ip = normalizeIp(ipPart);
        if (ip == null) {
            return;
        }

        long ipLong = ipToLong(ip);

        long mask = prefix == 0 ? 0 : (0xFFFFFFFFL << (32 - prefix)) & 0xFFFFFFFFL;
        long start = ipLong & mask;
        long end = start | (~mask & 0xFFFFFFFFL);

        out.add(new CIDRRange(start, end));
    }

    /**
     * 合并区间
     */
    private List<CIDRRange> merge(List<CIDRRange> list) {
        int size = list.size();
        if (size <= 1) {
            return list;
        }

        List<CIDRRange> merged = new ArrayList<>(size);
        CIDRRange current = list.get(0);

        for (int i = 1; i < size; i++) {
            CIDRRange next = list.get(i);
            if (next.getStartIp() <= current.getEndIp() + 1) {
                long end = Math.max(current.getEndIp(), next.getEndIp());
                current = new CIDRRange(current.getStartIp(), end);
            } else {
                merged.add(current);
                current = next;
            }
        }

        merged.add(current);
        return merged;
    }

    /**
     * 是否允许访问
     */
    public boolean isAllowed(String ip) {
        boolean match = matches(ip);
        return allowMode ? match : !match;
    }

    /**
     * 是否命中 CIDR
     */
    public boolean matches(String ip) {
        List<CIDRRange> local = this.ranges;
        // 快速失败
        if (local.isEmpty()) {
            return false;
        }

        String normalized = normalizeIp(ip);
        if (normalized == null) {
            return false;
        }

        long ipLong;
        try {
            ipLong = ipToLong(normalized);
        } catch (Exception e) {
            return false;
        }

        return binarySearch(local, ipLong);
    }

    /**
     * 二分查找
     */
    private boolean binarySearch(List<CIDRRange> ranges, long ip) {
        int left = 0;
        int right = ranges.size() - 1;

        while (left <= right) {
            int mid = (left + right) >>> 1;
            CIDRRange r = ranges.get(mid);

            if (ip < r.getStartIp()) {
                right = mid - 1;
            } else if (ip > r.getEndIp()) {
                left = mid + 1;
            } else {
                return true;
            }
        }

        return false;
    }

    /**
     * IP 转 long
     */
    private long ipToLong(String ip) {
        long result = 0;
        int part = 0;
        int shift = 24;

        for (int i = 0; i < ip.length(); i++) {
            char c = ip.charAt(i);
            if (c == '.') {
                result |= ((long) part << shift);
                shift -= 8;
                part = 0;
            } else {
                int d = c - '0';
                if (d < 0 || d > 9) {
                    throw new IllegalArgumentException();
                }
                part = part * 10 + d;
            }
        }

        result |= part;
        return result & 0xFFFFFFFFL;
    }

    /**
     * 安全 parse int（避免 substring + trim）
     */
    private int parseIntSafe(String s, int start) {
        int val = 0;
        for (int i = start; i < s.length(); i++) {
            char c = s.charAt(i);
            int d = c - '0';
            if (d < 0 || d > 9) {
                return -1;
            }
            val = val * 10 + d;
        }
        return val;
    }

    /**
     * IP 规范化
     */
    private String normalizeIp(String ip) {
        if (ip == null) {
            return null;
        }

        int len = ip.length();
        if (len == 0) {
            return null;
        }

        // 去首尾空格（避免 trim 产生新对象）
        int start = 0;
        int end = len - 1;

        while (start <= end && ip.charAt(start) <= ' ') start++;
        while (end >= start && ip.charAt(end) <= ' ') end--;

        if (start > end) {
            return null;
        }

        String result = (start == 0 && end == len - 1) ? ip : ip.substring(start, end + 1);

        if ("localhost".equalsIgnoreCase(result)) {
            return "127.0.0.1";
        }

        return result;
    }
}