import { ProtocolType } from '@/enums/etp/business'
import type { ProxyConfigProtocol } from '@/views/etp/plugin/menus'
import {
  fetchGetHttpProxyById,
  fetchGetHttpsProxyById,
  fetchGetTcpProxyById,
  fetchUpdateHttpProxy,
  fetchUpdateHttpsProxy,
  fetchUpdateTcpProxy
} from './proxy'

export type ProxyDetail =
  | Api.Proxy.HttpProxyDetailDTO
  | Api.Proxy.HttpsProxyDetailDTO
  | Api.Proxy.TcpProxyDetailDTO

const GET_API = {
  [ProtocolType.HTTP]: fetchGetHttpProxyById,
  [ProtocolType.HTTPS]: fetchGetHttpsProxyById,
  [ProtocolType.TCP]: fetchGetTcpProxyById
}

export function fetchProxyDetail(protocol: ProxyConfigProtocol, id: string) {
  return GET_API[protocol](id) as Promise<ProxyDetail>
}

function buildBandwidthPayload(detail: ProxyDetail): Api.Proxy.BandwidthSaveParam | null {
  const bandwidth = detail.bandwidth
  if (!bandwidth) return null

  const { limitTotal, limitIn, limitOut } = bandwidth
  if (limitTotal == null && limitIn == null && limitOut == null) return null

  return { limitTotal, limitIn, limitOut, unit: 'bps' }
}

function buildUpdatePayload(
  detail: ProxyDetail,
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
    deploymentMode: detail.deploymentMode,
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

async function submitUpdate(
  protocol: ProxyConfigProtocol,
  detail: ProxyDetail,
  payload: ReturnType<typeof buildUpdatePayload>
) {
  if (protocol === ProtocolType.TCP) {
    return fetchUpdateTcpProxy({ ...payload, remotePort: detail.listenPort })
  }

  return protocol === ProtocolType.HTTPS
    ? fetchUpdateHttpsProxy({ ...payload, domainType: detail.domainType, domains: detail.domains })
    : fetchUpdateHttpProxy({ ...payload, domainType: detail.domainType, domains: detail.domains })
}

export function saveProxyClusterConfig(
  protocol: ProxyConfigProtocol,
  detail: ProxyDetail,
  targets: Api.Proxy.ProxyTargetAddParam[],
  loadBalance: Api.Proxy.LoadBalanceParam
) {
  return submitUpdate(protocol, detail, buildUpdatePayload(detail, { targets, loadBalance }))
}

export function saveProxyTransportConfig(
  protocol: ProxyConfigProtocol,
  detail: ProxyDetail,
  transport: Api.Proxy.TransportSaveParam
) {
  return submitUpdate(protocol, detail, buildUpdatePayload(detail, { transport }))
}

export function saveProxyBandwidthConfig(
  protocol: ProxyConfigProtocol,
  detail: ProxyDetail,
  bandwidth: Api.Proxy.BandwidthSaveParam
) {
  return submitUpdate(protocol, detail, buildUpdatePayload(detail, { bandwidth }))
}
