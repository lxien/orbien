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

declare namespace Api.Proxy {
  /** 目标地址 */
  interface TargetDTO {
    id: number
    proxyId: string
    host: string
    port: number
    weight: number
    name: string
    /** 健康状态：1 正常，0 异常，未返回时为未检测 */
    healthStatus?: number
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

  /** UDP 代理列表 */
  interface UdpProxyListDTO extends ProxyListDTO {
    listenPort: number
  }

  /** 代理详情基础信息 */
  interface ProxyDetailDTO {
    id: string
    agentId: string
    name: string
    protocol: number
    agentType: number
    status: number
    transport: TransportDTO
    bandwidth: BandwidthDTO | null
    loadBalance: LoadBalanceDTO | null
    targets: TargetDTO[]
    createdAt: string
    updatedAt: string
  }

  /** 子域名绑定 */
  interface SubdomainBindingParam {
    rootDomainId?: number
    prefix: string
  }

  /** 子域名绑定详情 */
  interface SubdomainBindingDTO extends SubdomainBindingParam {
    rootDomain?: string
  }

  /** HTTP 代理详情 */
  interface HttpProxyDetailDTO {
    id: string
    agentId: string
    name: string
    domainType: number
    /** 完整自定义域名，仅 domainType=CUSTOM_DOMAIN 时有值 */
    customDomains?: string[]
    /** 子域名绑定，仅 domainType=SUBDOMAIN 时有值 */
    subdomainBindings?: SubdomainBindingDTO[]
    /** 全部内网后端（用于判断是否负载均衡模式） */
    targets?: TargetDTO[]
    loadBalance?: LoadBalanceDTO | null
    localHost: string
    localPort: number
    /** 总带宽 Mbps */
    limitTotal: number | null
    createdAt: string
    updatedAt: string
  }

  /** HTTPS 代理详情 */
  interface HttpsProxyDetailDTO extends HttpProxyDetailDTO {
    forceHttps?: boolean
  }

  /** TCP 代理详情 */
  interface TcpProxyDetailDTO {
    id: string
    agentId: string
    name: string
    /** 用户指定的远程端口，自动分配时为 null */
    remotePort: number | null
    /** 实际监听端口 */
    listenPort: number
    /** 全部内网后端（用于判断是否负载均衡模式） */
    targets?: TargetDTO[]
    loadBalance?: LoadBalanceDTO | null
    localHost: string
    localPort: number
    /** 总带宽 Mbps */
    limitTotal: number | null
    createdAt: string
    updatedAt: string
  }

  /** UDP 代理详情 */
  interface UdpProxyDetailDTO {
    id: string
    agentId: string
    name: string
    remotePort: number | null
    listenPort: number
    /** 全部内网后端（用于判断是否负载均衡模式） */
    targets?: TargetDTO[]
    loadBalance?: LoadBalanceDTO | null
    localHost: string
    localPort: number
    limitTotal: number | null
    createdAt: string
    updatedAt: string
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
    domainType: number
    subdomainBindings?: SubdomainBindingParam[]
    customDomains?: string[]
    localHost: string
    localPort: number
    limitTotal?: number
  }

  /** HTTP 代理更新参数 */
  interface HttpProxyUpdateParam {
    id: string
    name: string
    domainType: number
    subdomainBindings?: SubdomainBindingParam[]
    customDomains?: string[]
    localHost: string
    localPort: number
    limitTotal?: number
  }

  /** HTTPS 代理创建参数 */
  interface HttpsProxyCreateParam {
    agentId: string
    name: string
    domainType: number
    subdomainBindings?: SubdomainBindingParam[]
    customDomains?: string[]
    localHost: string
    localPort: number
    forceHttps?: boolean
    limitTotal?: number
  }

  /** HTTPS 代理更新参数 */
  interface HttpsProxyUpdateParam {
    id: string
    name: string
    domainType: number
    subdomainBindings?: SubdomainBindingParam[]
    customDomains?: string[]
    localHost: string
    localPort: number
    forceHttps?: boolean
    limitTotal?: number
  }

  /** TCP 代理创建参数 */
  interface TcpProxyCreateParam {
    agentId: string
    name: string
    localHost: string
    localPort: number
    remotePort?: number
    limitTotal?: number
  }

  /** TCP 代理更新参数 */
  interface TcpProxyUpdateParam {
    id: string
    name: string
    localHost: string
    localPort: number
    remotePort?: number
    limitTotal?: number
  }

  /** UDP 代理创建参数 */
  interface UdpProxyCreateParam {
    agentId: string
    name: string
    localHost: string
    localPort: number
    remotePort?: number
    limitTotal?: number
  }

  /** UDP 代理更新参数 */
  interface UdpProxyUpdateParam {
    id: string
    name: string
    localHost: string
    localPort: number
    remotePort?: number
    limitTotal?: number
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
