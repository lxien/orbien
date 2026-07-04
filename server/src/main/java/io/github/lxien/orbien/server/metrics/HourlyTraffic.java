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

package io.github.lxien.orbien.server.metrics;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 单个自然小时的流量统计值。
 */
@Getter
@Setter
public class HourlyTraffic {

    /** 小时桶起始时间（整点）。 */
    private LocalDateTime hour;

    /** 入站字节数。 */
    private long readBytes;

    /** 出站字节数。 */
    private long writeBytes;

    /** 入站消息数。 */
    private long readMessages;

    /** 出站消息数。 */
    private long writeMessages;

    /**
     * @param hour          小时桶起始时间
     * @param readBytes     入站字节数
     * @param writeBytes    出站字节数
     * @param readMessages  入站消息数
     * @param writeMessages 出站消息数
     */
    public HourlyTraffic(LocalDateTime hour, long readBytes, long writeBytes, long readMessages, long writeMessages) {
        this.hour = hour;
        this.readBytes = readBytes;
        this.writeBytes = writeBytes;
        this.readMessages = readMessages;
        this.writeMessages = writeMessages;
    }
}
