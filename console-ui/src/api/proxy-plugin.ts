import { ProtocolType, getProtocolLabel } from '@/enums/orbien/business'
import type { ProxyConfigProtocol } from '@/views/orbien/plugin/menus'
import {
  fetchGetHttpProxyById,
  fetchGetHttpsProxyById,
  fetchGetTcpProxyById,
  fetchGetUdpProxyById,
  fetchUpdateHttpProxy,
  fetchUpdateHttpsProxy,
  fetchUpdateTcpProxy,
  fetchUpdateUdpProxy
} from './proxy'

export type ProxyDetail =
  | Api.Proxy.HttpProxyDetailDTO
  | Api.Proxy.HttpsProxyDetailDTO
  | Api.Proxy.TcpProxyDetailDTO
  | Api.Proxy.UdpProxyDetailDTO

/** 扩展设置插件使用的完整 HTTP 详情结构，待独立接口提供 */
interface HttpPluginDetail {
  id: string
  name: string
  status: number
  domainType: number
  domains?: string[]
  forceHttps?: boolean
  targets: Api.Proxy.TargetDTO[]
  transport: Api.Proxy.TransportDTO
  bandwidth: Api.Proxy.BandwidthDTO | null
  loadBalance: Api.Proxy.LoadBalanceDTO | null
}

const GET_API = {
  [ProtocolType.HTTP]: fetchGetHttpProxyById,
  [ProtocolType.HTTPS]: fetchGetHttpsProxyById,
  [ProtocolType.TCP]: fetchGetTcpProxyById,
  [ProtocolType.UDP]: fetchGetUdpProxyById
}

export function fetchProxyDetail(protocol: ProxyConfigProtocol, id: string) {
  return GET_API[protocol](id) as Promise<ProxyDetail>
}

function asHttpPluginDetail(detail: ProxyDetail): HttpPluginDetail | null {
  if (!('targets' in detail)) {
    return null
  }
  const candidate = detail as unknown as HttpPluginDetail
  return Array.isArray(candidate.targets) ? candidate : null
}

function buildBandwidthPayload(detail: HttpPluginDetail): Api.Proxy.BandwidthSaveParam | null {
  const bandwidth = detail.bandwidth
  if (!bandwidth) return null

  const { limitTotal, limitIn, limitOut } = bandwidth
  if (limitTotal == null && limitIn == null && limitOut == null) return null

  return { limitTotal, limitIn, limitOut, unit: 'bps' }
}

function buildHttpUpdatePayload(
  detail: HttpPluginDetail,
  overrides: Partial<{
    targets: Api.Proxy.ProxyTargetAddParam[]
    loadBalance: Api.Proxy.LoadBalanceParam | null
    transport: Api.Proxy.TransportSaveParam
    bandwidth: Api.Proxy.BandwidthSaveParam | null
  }> = {}
) {
  return {
    id: detail.id,
    name: detail.name,
    status: detail.status,
    targets:
      overrides.targets ??
      detail.targets.map(({ host, port, weight, name }) => ({
        host,
        port,
        weight,
        name
      })),
    loadBalance: overrides.loadBalance ?? detail.loadBalance,
    bandwidth:
      overrides.bandwidth !== undefined ? overrides.bandwidth : buildBandwidthPayload(detail),
    transport: overrides.transport ?? detail.transport
  }
}

function buildTcpUpdatePayload(
  detail: Api.Proxy.TcpProxyDetailDTO,
  limitTotal?: number
): Api.Proxy.TcpProxyUpdateParam {
  return {
    id: detail.id,
    name: detail.name,
    localHost: detail.localHost,
    localPort: detail.localPort,
    ...(detail.remotePort != null ? { remotePort: detail.remotePort } : {}),
    ...(limitTotal != null ? { limitTotal } : detail.limitTotal != null ? { limitTotal: detail.limitTotal } : {})
  }
}

async function submitHttpUpdate(
  protocol: ProxyConfigProtocol,
  detail: HttpPluginDetail,
  payload: ReturnType<typeof buildHttpUpdatePayload>
) {
  if (protocol === ProtocolType.HTTPS) {
    return fetchUpdateHttpsProxy({
      id: detail.id,
      name: detail.name,
      domainType: detail.domainType,
      localHost: payload.targets[0]?.host ?? '',
      localPort: payload.targets[0]?.port ?? 0,
      forceHttps: detail.forceHttps
    })
  }

  return fetchUpdateHttpProxy({
    id: detail.id,
    name: detail.name,
    domainType: detail.domainType,
    localHost: payload.targets[0]?.host ?? '',
    localPort: payload.targets[0]?.port ?? 0,
    limitTotal: detail.bandwidth?.limitTotal != null ? Math.round(detail.bandwidth.limitTotal / 1_000_000) : undefined
  })
}

function buildUdpUpdatePayload(
  detail: Api.Proxy.UdpProxyDetailDTO,
  limitTotal?: number
): Api.Proxy.UdpProxyUpdateParam {
  return {
    id: detail.id,
    name: detail.name,
    localHost: detail.localHost,
    localPort: detail.localPort,
    ...(detail.remotePort != null ? { remotePort: detail.remotePort } : {}),
    ...(limitTotal != null ? { limitTotal } : detail.limitTotal != null ? { limitTotal: detail.limitTotal } : {})
  }
}

function rejectPluginSave(protocol: ProxyConfigProtocol, feature: string) {
  if (protocol === ProtocolType.TCP || protocol === ProtocolType.UDP) {
    return Promise.reject(new Error(`${getProtocolLabel(protocol)} ${feature}暂未适配新接口`))
  }
  return Promise.reject(new Error(`HTTP/HTTPS ${feature}暂未适配新接口`))
}

export function saveProxyClusterConfig(
  protocol: ProxyConfigProtocol,
  detail: ProxyDetail,
  targets: Api.Proxy.ProxyTargetAddParam[],
  loadBalance: Api.Proxy.LoadBalanceParam
) {
  const pluginDetail = asHttpPluginDetail(detail)
  if (!pluginDetail) {
    return rejectPluginSave(protocol, '负载均衡配置')
  }
  return submitHttpUpdate(
    protocol,
    pluginDetail,
    buildHttpUpdatePayload(pluginDetail, { targets, loadBalance })
  )
}

export function saveProxyTransportConfig(
  protocol: ProxyConfigProtocol,
  detail: ProxyDetail,
  transport: Api.Proxy.TransportSaveParam
) {
  const pluginDetail = asHttpPluginDetail(detail)
  if (!pluginDetail) {
    return rejectPluginSave(protocol, '传输配置')
  }
  return submitHttpUpdate(protocol, pluginDetail, buildHttpUpdatePayload(pluginDetail, { transport }))
}

export function saveProxyBandwidthConfig(
  protocol: ProxyConfigProtocol,
  detail: ProxyDetail,
  bandwidth: Api.Proxy.BandwidthSaveParam
) {
  if (protocol === ProtocolType.TCP) {
    const tcpDetail = detail as Api.Proxy.TcpProxyDetailDTO
    const limitTotalMbps =
      bandwidth.limitTotal != null && bandwidth.unit
        ? Math.round(bandwidth.limitTotal / 1_000_000)
        : tcpDetail.limitTotal ?? undefined
    return fetchUpdateTcpProxy(buildTcpUpdatePayload(tcpDetail, limitTotalMbps))
  }

  if (protocol === ProtocolType.UDP) {
    const udpDetail = detail as Api.Proxy.UdpProxyDetailDTO
    const limitTotalMbps =
      bandwidth.limitTotal != null && bandwidth.unit
        ? Math.round(bandwidth.limitTotal / 1_000_000)
        : udpDetail.limitTotal ?? undefined
    return fetchUpdateUdpProxy(buildUdpUpdatePayload(udpDetail, limitTotalMbps))
  }

  const pluginDetail = asHttpPluginDetail(detail)
  if (!pluginDetail) {
    return rejectPluginSave(protocol, '带宽配置')
  }
  return submitHttpUpdate(protocol, pluginDetail, buildHttpUpdatePayload(pluginDetail, { bandwidth }))
}
