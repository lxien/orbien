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

package io.github.lxien.orbien.server.web.common.monitor;
/**
 * 字节格式化
 */
public class ByteUtils {
    /**
     * 将字节数格式化为带单位的字符串（KB/MB/GB/TB）
     */
    public static String formatBytes(long bytes) {
        if (bytes <= 0) return "0MB";
        double value = bytes;
        String unit = "B";
        if (value >= 1024L * 1024 * 1024 * 1024L) {
            value /= (1024.0 * 1024 * 1024 * 1024);
            unit = "TB";
        } else if (value >= 1024L * 1024 * 1024) {
            value /= (1024.0 * 1024 * 1024);
            unit = "GB";
        } else if (value >= 1024L * 1024) {
            value /= (1024.0 * 1024);
            unit = "MB";
        } else if (value >= 1024L) {
            value /= 1024.0;
            unit = "KB";
        }
        if (value == Math.floor(value)) {
            return (int) value + unit;
        } else {
            String formatted = String.format("%.2f", value).replaceAll("\\.?0*$", "");
            return formatted + unit;
        }
    }
}
