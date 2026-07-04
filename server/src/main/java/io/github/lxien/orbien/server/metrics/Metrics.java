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

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 代理实时指标快照。
 *
 * <p>由 {@link ProxyMetrics#toMetrics()} 生成，供查询接口返回。
 */
@Data
public class Metrics {

    /** 代理标识。 */
    private String proxyId;

    /** 当前活跃隧道通道数。 */
    private int activeChannels;

    /** 累计入站字节数。 */
    private long readBytes;

    /** 累计出站字节数。 */
    private long writeBytes;

    /** 累计入站消息数。 */
    private long readMessages;

    /** 累计出站消息数。 */
    private long writeMessages;

    /** 入站字节速率，单位：字节/秒。 */
    private double readRate;

    /** 出站字节速率，单位：字节/秒。 */
    private double writeRate;

    /** 最近一次产生流量的时间。 */
    private LocalDateTime lastActiveTime;
}
