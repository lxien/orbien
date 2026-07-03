/** ETP 业务枚举 */

export enum ProtocolType {
  TCP = 1,
  HTTP = 2,
  HTTPS = 3,
  UDP = 4
}

export enum PortPoolType {
  TCP = 1,
  UDP = 2
}

export enum ProxyStatus {
  CLOSED = 0,
  OPEN = 1
}

export enum DomainType {
  AUTO = 0,
  SUBDOMAIN = 1,
  CUSTOM_DOMAIN = 2
}

export enum AccessControl {
  DENY = 0,
  ALLOW = 1
}

export enum HealthCheckType {
  TCP = 0,
  HTTP = 1
}

export enum LoadBalanceType {
  ROUND_ROBIN = 1,
  WEIGHT = 2,
  RANDOM = 3,
  LEAST_CONN = 4
}

export enum AgentType {
  EMBEDDED = 0,
  STANDALONE = 1
}

export enum BandwidthUnit {
  BPS = 'bps',
  KBPS = 'Kbps',
  MBPS = 'Mbps',
  GBPS = 'Gbps'
}

export enum TunnelType {
  MULTIPLEX = 0,
  DIRECT = 1
}

export enum TargetHealthStatus {
  DOWN = 0,
  UP = 1
}

export const PORT_POOL_TYPE_OPTIONS = [
  { label: 'TCP', value: PortPoolType.TCP },
  { label: 'UDP', value: PortPoolType.UDP }
] as const

export const LOAD_BALANCE_OPTIONS = [
  { label: '轮询 (roundrobin)', value: LoadBalanceType.ROUND_ROBIN },
  { label: '权重 (weight)', value: LoadBalanceType.WEIGHT },
  { label: '随机 (random)', value: LoadBalanceType.RANDOM },
  { label: '最少连接 (leastconn)', value: LoadBalanceType.LEAST_CONN }
] as const

export const BANDWIDTH_UNIT_OPTIONS = [
  BandwidthUnit.KBPS,
  BandwidthUnit.MBPS,
  BandwidthUnit.GBPS
] as const

export const BANDWIDTH_UNIT_TO_BPS: Record<BandwidthUnit, number> = {
  [BandwidthUnit.BPS]: 1,
  [BandwidthUnit.KBPS]: 1_000,
  [BandwidthUnit.MBPS]: 1_000_000,
  [BandwidthUnit.GBPS]: 1_000_000_000
}

export function getProtocolLabel(protocol?: number) {
  switch (protocol) {
    case ProtocolType.TCP:
      return 'TCP'
    case ProtocolType.HTTP:
      return 'HTTP'
    case ProtocolType.HTTPS:
      return 'HTTPS'
    case ProtocolType.UDP:
      return 'UDP'
    default:
      return ''
  }
}

export function getDomainTypeLabel(domainType: number) {
  switch (domainType) {
    case DomainType.CUSTOM_DOMAIN:
      return { type: 'warning' as const, text: '自定义' }
    case DomainType.SUBDOMAIN:
      return { type: 'primary' as const, text: '子域名' }
    default:
      return { type: 'info' as const, text: '自动' }
  }
}

export function getAgentTypeLabel(agentType?: number) {
  return agentType === AgentType.STANDALONE ? 'Standalone' : 'Embedded'
}

export function getPortPoolTypeLabel(type: number) {
  switch (type) {
    case PortPoolType.TCP:
      return { type: 'primary' as const, text: 'TCP' }
    case PortPoolType.UDP:
      return { type: 'warning' as const, text: 'UDP' }
    default:
      return { type: 'info' as const, text: '未知' }
  }
}
