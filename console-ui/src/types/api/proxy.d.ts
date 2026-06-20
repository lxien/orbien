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

declare namespace Api.Proxy {
  /** 目标地址 */
  interface TargetDTO {
    id: number
    proxyId: string
    host: string
    port: number
    weight: number
    name: string
  }

  /** 带宽配置 */
  interface BandwidthDTO {
    limitTotal: number | null
    limitIn: number | null
    limitOut: number | null
  }

  /** 负载均衡配置 */
  interface LoadBalanceDTO {
    strategy: number
  }

  /** 传输配置 */
  interface TransportDTO {
    encrypt: boolean
    tunnelType: number
  }

  /** 代理列表基础信息 */
  interface ProxyListDTO {
    id: string
    agentId: string
    name: string
    protocol: number
    agentType: number
    status: number
    targets: TargetDTO[]
  }

  /** HTTP 代理列表 */
  interface HttpProxyListDTO extends ProxyListDTO {
    domains: string[]
    httpProxyPort: number
  }

  /** HTTPS 代理列表 */
  interface HttpsProxyListDTO extends ProxyListDTO {
    domains: string[]
    httpsProxyPort: number
  }

  /** TCP 代理列表 */
  interface TcpProxyListDTO extends ProxyListDTO {
    listenPort: number
  }

  /** 代理详情基础信息 */
  interface ProxyDetailDTO {
    id: string
    agentId: string
    name: string
    protocol: number
    agentType: number
    deploymentMode: number
    status: number
    transport: TransportDTO
    bandwidth: BandwidthDTO | null
    loadBalance: LoadBalanceDTO | null
    targets: TargetDTO[]
    createdAt: string
    updatedAt: string
  }

  /** HTTP 代理详情 */
  interface HttpProxyDetailDTO extends ProxyDetailDTO {
    domains: string[]
    domainType: number
  }

  /** HTTPS 代理详情 */
  interface HttpsProxyDetailDTO extends ProxyDetailDTO {
    domains: string[]
    domainType: number
  }

  /** TCP 代理详情 */
  interface TcpProxyDetailDTO extends ProxyDetailDTO {
    listenPort: number
  }

  /** 目标地址创建/更新参数 */
  interface ProxyTargetAddParam {
    host: string
    port: number
    weight: number
    name: string
  }

  /** 传输配置参数 */
  interface TransportSaveParam {
    encrypt: boolean
    tunnelType: number
  }

  /** 带宽配置参数 */
  interface BandwidthSaveParam {
    limitTotal: number | null
    limitIn: number | null
    limitOut: number | null
    unit: string | null
  }

  /** 负载均衡参数 */
  interface LoadBalanceParam {
    strategy: number
  }

  /** HTTP 代理创建参数 */
  interface HttpProxyCreateParam {
    agentId: string
    name: string
    status: number
    domainType: number
    domains: string[] | null
    deploymentMode: number
    targets: ProxyTargetAddParam[]
    transport: TransportSaveParam
    bandwidth: BandwidthSaveParam | null
    loadBalance: LoadBalanceParam | null
  }

  /** HTTP 代理更新参数 */
  interface HttpProxyUpdateParam {
    id: string
    name: string
    status: number
    domainType: number
    domains: string[] | null
    deploymentMode: number
    targets: ProxyTargetAddParam[]
    bandwidth: BandwidthSaveParam | null
    loadBalance: LoadBalanceParam | null
    transport: TransportSaveParam
  }

  /** HTTPS 代理创建参数 */
  interface HttpsProxyCreateParam {
    agentId: string
    name: string
    status: number
    domainType: number
    domains: string[] | null
    deploymentMode: number
    targets: ProxyTargetAddParam[]
    transport: TransportSaveParam
    bandwidth: BandwidthSaveParam | null
    loadBalance: LoadBalanceParam | null
  }

  /** HTTPS 代理更新参数 */
  interface HttpsProxyUpdateParam {
    id: string
    name: string
    status: number
    domainType: number
    domains: string[] | null
    deploymentMode: number
    targets: ProxyTargetAddParam[]
    bandwidth: BandwidthSaveParam | null
    loadBalance: LoadBalanceParam | null
    transport: TransportSaveParam
  }

  /** TCP 代理创建参数 */
  interface TcpProxyCreateParam {
    agentId: string
    name: string
    status: number
    deploymentMode: number
    targets: ProxyTargetAddParam[]
    bandwidth: BandwidthSaveParam | null
    loadBalance: LoadBalanceParam | null
    transport: TransportSaveParam
    remotePort: number | null
  }

  /** TCP 代理更新参数 */
  interface TcpProxyUpdateParam {
    id: string
    name: string
    status: number
    deploymentMode: number
    targets: ProxyTargetAddParam[]
    bandwidth: BandwidthSaveParam | null
    loadBalance: LoadBalanceParam | null
    transport: TransportSaveParam
    remotePort: number
  }

  /** 批量删除参数 */
  interface ProxyBatchDeleteParam {
    ids: string[]
    protocol: number
  }

  /** 状态更新参数 */
  interface ProxyStatusUpdateParam {
    status: number
  }
}
