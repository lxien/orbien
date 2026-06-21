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

declare namespace Api.Monitor {
  interface CpuInfo {
    total: number
    used: number | string
    usage: number | string
  }

  interface JvmMemoryInfo {
    total: string
    used: string
    usage: number | string
  }

  interface OsMemoryInfo {
    total: string
    used: string
    usage: number | string
  }

  interface DirectMemoryInfo {
    total: string
    used: string
    usage: number | string
  }

  interface ServerInfo {
    cpu: CpuInfo
    jvmMem: JvmMemoryInfo
    osMem: OsMemoryInfo
    directMem: DirectMemoryInfo
  }

  interface DashboardSummary {
    totalAgents: number
    onlineAgents: number
    totalProxies: number
    startedProxies: number
  }

  interface ProxyProtocolCountDTO {
    httpCount: number
    httpsCount: number
    tcpCount: number
  }
}
