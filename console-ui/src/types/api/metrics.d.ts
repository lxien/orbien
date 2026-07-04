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

declare namespace Api.Metrics {
  interface MetricsDTO {
    key: string
    channels: number
    readBytes: number
    writeBytes: number
    readMessages: number
    writeMessages: number
    time: string
  }

  interface Metrics24LineDTO {
    xAxis: string[]
    yAxis: number[]
  }

  interface TrafficCountDTO {
    proxyId: string
    proxyName?: string
    protocol?: number
    agentId?: string
    agentName?: string
    readBytes: number
    writeBytes: number
    readMessages: number
    writeMessages: number
    totalBytes: number
  }

  interface TrafficChartVO {
    up: Metrics24LineDTO
    down: Metrics24LineDTO
    upTotal: number
    downTotal: number
    downRate: number
    upRate: number
    /** 当前活跃连接数 */
    activeChannels?: number
    /** 时间刻度单位：hour=小时粒度，day=天粒度 */
    timeUnit?: 'hour' | 'day'
  }
}
