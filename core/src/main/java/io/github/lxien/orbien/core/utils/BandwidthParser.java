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

package io.github.lxien.orbien.core.utils;

import io.github.lxien.orbien.core.enums.BandwidthUnit;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 带宽限流解析器
 */
public final class BandwidthParser {

    /**
     * 严格匹配：
     * - 只允许正整数（不允许小数）
     * - 不允许前导0（0除外）
     * - 单位严格大小写
     */
    private static final Pattern STRICT_PATTERN = Pattern.compile("^(0|[1-9][0-9]*)(bps|Kbps|Mbps|Gbps)$");

    private BandwidthParser() {
    }

    /**
     * 将原始带宽值解析为bps单位值
     *
     * @param value 原始带宽，如 2Mbps
     * @return 转换后的值 单位：bps
     */
    public static Long parseToBps(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }

        Matcher matcher = STRICT_PATTERN.matcher(value.trim());
        if (!matcher.matches()) {
            throw new IllegalArgumentException(
                    "带宽格式无效: " + value +
                            "（必须为: 10Mbps / 100Kbps / 1Gbps，整数，无空格，区分大小写）"
            );
        }

        long number;
        try {
            number = Long.parseLong(matcher.group(1));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("数值过大: " + value);
        }

        String unitStr = matcher.group(2);
        BandwidthUnit unit = BandwidthUnit.fromCode(unitStr);

        long factor = unit.getFactor();
        if (number > Long.MAX_VALUE / factor) {
            throw new IllegalArgumentException("带宽值过大导致溢出: " + value);
        }

        return number * factor;
    }
}