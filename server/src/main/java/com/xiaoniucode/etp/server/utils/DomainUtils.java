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

package com.xiaoniucode.etp.server.utils;

import org.springframework.util.StringUtils;

/**
 * 域名工具类
 * 通用域名解析、拆分、前缀提取等操作
 *
 * @author xiaoniucode
 */
public class DomainUtils {

    /**
     * 根据根域名，从完整域名中提取真正的前缀部分
     *
     * @param fullDomain  完整域名，例如：api.user.example.com
     * @param rootDomain  配置的根域名，例如：example.com
     * @return 前缀部分（例如：api.user），如果不属于该根域名则返回 null
     *         如果 fullDomain 就是 rootDomain 本身，则返回空字符串 ""
     */
    public static String extractPrefix(String fullDomain, String rootDomain) {
        if (!StringUtils.hasText(fullDomain) || !StringUtils.hasText(rootDomain)) {
            return null;
        }

        String full = fullDomain.toLowerCase().trim();
        String base = rootDomain.toLowerCase().trim();

        // 移除末尾可能的点
        if (full.endsWith(".")) {
            full = full.substring(0, full.length() - 1);
        }
        if (base.endsWith(".")) {
            base = base.substring(0, base.length() - 1);
        }

        // 判断是否属于该根域名
        if (full.equals(base)) {
            return "";// 根域名本身，前缀为空
        }

        if (!full.endsWith("." + base)) {
            return null;// 不属于该根域名，返回 null
        }

        // 提取前缀部分
    return full.substring(0, full.length() - base.length() - 1); // -1 是去掉 "."
    }

    /**
     * 判断完整域名是否属于指定的根域名
     *
     * @param fullDomain 完整域名
     * @param rootDomain 根域名
     * @return true 如果属于该根域名
     */
    public static boolean isUnderBaseDomain(String fullDomain, String rootDomain) {
        String prefix = extractPrefix(fullDomain, rootDomain);
        return prefix != null;
    }

    /**
     * 获取域名层级（子域名层数）
     * 例如：api.user.example.com 的层级为 3（api + user + example.com）
     */
    public static int getDomainLevel(String domain) {
        if (!StringUtils.hasText(domain)) {
            return 0;
        }
        String clean = domain.toLowerCase().trim();
        if (clean.endsWith(".")) {
            clean = clean.substring(0, clean.length() - 1);
        }
        return clean.split("\\.").length;
    }
}