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

package com.xiaoniucode.etp.server.web.common.utils;

/**
 * 脱敏工具类
 *
 * @author xiaoniucode
 */
public class DesensitizedUtil {

    /**
     * Token令牌脱敏
     * 将指定范围的字符替换为*
     *
     * @param token  令牌字符串
     * @param prefix 保留前缀长度
     * @param suffix 保留后缀长度
     * @return 脱敏后的令牌
     */
    public static String token(String token, int prefix, int suffix) {
        if (token == null || token.isEmpty()) {
            return token;
        }
        int length = token.length();
        if (prefix < 0 || suffix < 0) {
            return token;
        }
        if (prefix + suffix >= length) {
            return token;
        }
        int maskLength = length - prefix - suffix;
        StringBuilder result = new StringBuilder();
        result.append(token, 0, prefix);
        result.repeat("*", Math.max(0, maskLength));
        result.append(token, length - suffix, length);
        return result.toString();
    }

    /**
     * Token令牌脱敏，默认保留前4位和后4位
     * 适用于32位或更长Token的快速脱敏
     *
     * @param token 令牌字符串
     * @return 脱敏后的令牌
     */
    public static String token(String token) {
        return token(token, 4, 4);
    }
}
