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

package io.github.lxien.orbien.core.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Getter
@AllArgsConstructor
public enum BandwidthUnit {

    /**
     * 比特每秒
     */
    BPS("bps", "比特每秒", 1L),

    /**
     * 千比特每秒
     */
    KBPS("Kbps", "千比特每秒", 1000L),

    /**
     * 兆比特每秒
     */
    MBPS("Mbps", "兆比特每秒", 1000L * 1000),

    /**
     * 吉比特每秒
     */
    GBPS("Gbps", "吉比特每秒", 1000L * 1000 * 1000);

    private final String code;
    private final String desc;

    /**
     * 转换倍率（统一转 bps）
     */
    private final long factor;

    /**
     * 转换为 bps
     */
    public long toBps(long value) {
        if (value < 0) {
            throw new IllegalArgumentException("带宽值不能为负");
        }
        return value * factor;
    }

    private static final Map<String, BandwidthUnit> MAP = new HashMap<>();

    static {
        for (BandwidthUnit unit : values()) {
            MAP.put(unit.code.toLowerCase(), unit);
        }
    }

    public static BandwidthUnit fromCode(String code) {
        if (code == null || code.isEmpty()) {
            throw new IllegalArgumentException("带宽单位代码不能为null或空");
        }
        BandwidthUnit unit = MAP.get(code.toLowerCase());
        if (unit == null) {
            throw new IllegalArgumentException("未知带宽单位代码: " + code);
        }
        return unit;
    }
}